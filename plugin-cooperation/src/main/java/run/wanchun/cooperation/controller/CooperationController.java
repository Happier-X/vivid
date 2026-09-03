package run.wanchun.cooperation.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.ReactiveExtensionClient;
import run.wanchun.cooperation.config.CooperationProperties;
import run.wanchun.cooperation.dto.CooperationRequest;
import run.wanchun.cooperation.dto.CooperationResponse;
import run.wanchun.cooperation.extension.Cooperation;
import run.wanchun.cooperation.service.EmailService;
import run.wanchun.cooperation.service.RateLimiter;

/**
 * 合作咨询路由：POST 匿名可访问，GET/DELETE/PUT 需管理员鉴权。
 * 扩展功能：列表筛选（keyword/type/handled/时间区间）、分页、删除、标记已处理、导出 CSV。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CooperationController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "institution", "community", "home_government", "channel_oem");
    private static final String PHONE_RE = "^(1[3-9]\\d{9}|0\\d{2,3}-?\\d{7,8})$";

    private final ReactiveExtensionClient extensionClient;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;
    private final CooperationProperties properties;

    @Bean
    RouterFunction<ServerResponse> cooperationRouter() {
        return route()
            .POST("/apis/api.cooperation.vivid.run/v1alpha1/cooperations",
                contentType(MediaType.APPLICATION_JSON), this::handleCreate)
            .GET("/apis/api.cooperation.vivid.run/v1alpha1/cooperations/export", this::handleExport)
            .GET("/apis/api.cooperation.vivid.run/v1alpha1/cooperations", this::handleList)
            .GET("/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}", this::handleGet)
            .DELETE("/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}", this::handleDelete)
            .PUT("/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}/handled",
                this::handleUpdateHandled)
            .build();
    }

    private Mono<ServerResponse> handleCreate(ServerRequest request) {
        String ip = getClientIp(request.exchange());
        int rateLimitSeconds = properties.getRateLimitSeconds() != null
            ? properties.getRateLimitSeconds() : 60;

        return request.bodyToMono(CooperationRequest.class)
            .flatMap(req -> {
                // 蜜罐：有值直接静默成功，不落库不发邮件
                if (req.getWebsite() != null && !req.getWebsite().trim().isEmpty()) {
                    log.info("蜜罐命中，IP={}，静默成功", ip);
                    return ok().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.ok("提交成功"));
                }

                // 服务端校验
                String validationError = validate(req);
                if (validationError != null) {
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.fail(validationError));
                }

                // 限流
                if (!rateLimiter.tryAcquire(ip, rateLimitSeconds)) {
                    long retryAfter = rateLimiter.getRetryAfterSeconds(ip, rateLimitSeconds);
                    String msg = retryAfter > 0
                        ? "提交过于频繁，请 " + retryAfter + " 秒后再试"
                        : "提交过于频繁，请稍后重试";
                    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.fail(msg));
                }

                // 构建 Extension
                Cooperation cooperation = new Cooperation();
                Cooperation.CooperationSpec spec = new Cooperation.CooperationSpec();
                spec.setCompany(req.getCompany().trim());
                spec.setContact(req.getContact().trim());
                spec.setPhone(req.getPhone().trim());
                spec.setType(req.getType().trim());
                spec.setTypeLabel(req.getTypeLabel() != null ? req.getTypeLabel().trim() : req.getType().trim());
                spec.setMessage(req.getMessage() != null ? req.getMessage().trim() : "");
                spec.setWebsite("");
                spec.setSourceUrl(req.getSourceUrl());
                spec.setUserAgent(req.getUserAgent());
                spec.setTimestamp(req.getTimestamp());
                spec.setIp(ip);
                spec.setHandled(false);
                cooperation.setSpec(spec);

                // 落库 -> 发邮件
                return extensionClient.create(cooperation)
                    .flatMap(created -> emailService.sendCooperationEmail(created)
                        .thenReturn(created)
                        .onErrorResume(e -> {
                            log.error("邮件发送失败，但记录已落库", e);
                            return Mono.error(new RuntimeException("邮件发送失败，请稍后重试"));
                        }))
                    .flatMap(created -> ok().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.ok("提交成功")))
                    .onErrorResume(e -> {
                        if (e.getMessage() != null && e.getMessage().contains("邮件发送失败")) {
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(CooperationResponse.fail("邮件发送失败，请稍后重试"));
                        }
                        log.error("创建合作记录失败", e);
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(CooperationResponse.fail("提交失败，请稍后重试"));
                    });
            })
            .onErrorResume(e -> {
                log.error("解析请求失败", e);
                return ServerResponse.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(CooperationResponse.fail("请求参数错误"));
            });
    }

    private Mono<ServerResponse> handleList(ServerRequest request) {
        return checkAdmin()
            .flatMap(authResult -> {
                if (authResult != AuthResult.ADMIN) {
                    return authResult.toResponse();
                }
                int pageTmp = parseInt(request.queryParam("page").orElse("0"), 0);
                int sizeTmp = parseInt(request.queryParam("size").orElse("20"), 20);
                int pageVal = Math.max(0, pageTmp);
                int sizeVal = Math.max(1, Math.min(sizeTmp, 50));
                String keyword = request.queryParam("keyword").orElse("").trim();
                String type = request.queryParam("type").orElse("").trim();
                String handled = request.queryParam("handled").orElse("all").trim();
                String startTime = request.queryParam("startTime").orElse("").trim();
                String endTime = request.queryParam("endTime").orElse("").trim();

                Comparator<Cooperation> comparator = Comparator
                    .comparing((Cooperation c) -> {
                        if (c.getMetadata() == null) return null;
                        return c.getMetadata().getCreationTimestamp();
                    }, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();
                final int finalPage = pageVal;
                final int finalSize = sizeVal;
                return extensionClient.list(Cooperation.class, null, comparator)
                    .collectList()
                    .flatMap(all -> {
                        List<Cooperation> filtered = applyFilters(all, keyword, type, handled, startTime, endTime);
                        // 按创建时间倒序已在 comparator 完成，若未排序则再次排序确保
                        filtered.sort(comparator);
                        int total = filtered.size();
                        int from = Math.min(finalPage * finalSize, total);
                        int to = Math.min(from + finalSize, total);
                        List<Cooperation> pageItems = filtered.subList(from, to);
                        ListResult<Cooperation> result = new ListResult<>(finalPage, finalSize, total, pageItems);
                        return ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result);
                    });
            });
    }

    private Mono<ServerResponse> handleGet(ServerRequest request) {
        return checkAdmin()
            .flatMap(authResult -> {
                if (authResult != AuthResult.ADMIN) {
                    return authResult.toResponse();
                }
                String name = request.pathVariable("name");
                return extensionClient.get(Cooperation.class, name)
                    .flatMap(coop -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(coop))
                    .switchIfEmpty(ServerResponse.notFound().build());
            });
    }

    private Mono<ServerResponse> handleDelete(ServerRequest request) {
        return checkAdmin()
            .flatMap(authResult -> {
                if (authResult != AuthResult.ADMIN) {
                    return authResult.toResponse();
                }
                String name = request.pathVariable("name");
                return extensionClient.get(Cooperation.class, name)
                    .flatMap(existing -> extensionClient.delete(existing)
                        .then(Mono.defer(() -> ok().contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(CooperationResponse.ok("删除成功")))))
                    .switchIfEmpty(ServerResponse.notFound().build());
            });
    }

    @SuppressWarnings("unchecked")
    private Mono<ServerResponse> handleUpdateHandled(ServerRequest request) {
        return checkAdmin()
            .flatMap(authResult -> {
                if (authResult != AuthResult.ADMIN) {
                    return authResult.toResponse();
                }
                String name = request.pathVariable("name");
                return request.bodyToMono(Map.class)
                    .flatMap(map -> {
                        Object raw = map.get("handled");
                        if (raw == null) {
                            return ServerResponse.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(CooperationResponse.fail("handled 字段为必填"));
                        }
                        boolean handled;
                        if (raw instanceof Boolean b) {
                            handled = b;
                        } else {
                            String s = String.valueOf(raw).trim();
                            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                                handled = true;
                            } else if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                                handled = false;
                            } else {
                                return ServerResponse.status(HttpStatus.BAD_REQUEST)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(CooperationResponse.fail("handled 必须为布尔值"));
                            }
                        }
                        return extensionClient.get(Cooperation.class, name)
                            .flatMap(existing -> {
                                if (existing.getSpec() == null) {
                                    existing.setSpec(new Cooperation.CooperationSpec());
                                }
                                existing.getSpec().setHandled(handled);
                                return extensionClient.update(existing);
                            })
                            .flatMap(updated -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(updated))
                            .switchIfEmpty(ServerResponse.notFound().build());
                    })
                    .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.fail("请求体不能为空")));
            });
    }

    private Mono<ServerResponse> handleExport(ServerRequest request) {
        return checkAdmin()
            .flatMap(authResult -> {
                if (authResult != AuthResult.ADMIN) {
                    return authResult.toResponse();
                }
                String keyword = request.queryParam("keyword").orElse("").trim();
                String type = request.queryParam("type").orElse("").trim();
                String handled = request.queryParam("handled").orElse("all").trim();
                String startTime = request.queryParam("startTime").orElse("").trim();
                String endTime = request.queryParam("endTime").orElse("").trim();

                Comparator<Cooperation> comparator = Comparator
                    .comparing((Cooperation c) -> {
                        if (c.getMetadata() == null) return null;
                        return c.getMetadata().getCreationTimestamp();
                    }, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed();

                return extensionClient.list(Cooperation.class, null, comparator)
                    .collectList()
                    .flatMap(all -> {
                        List<Cooperation> filtered = applyFilters(all, keyword, type, handled, startTime, endTime);
                        filtered.sort(comparator);
                        String csv = generateCsv(filtered);
                        String filename = "cooperations-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                            .withZone(ZoneOffset.UTC).format(Instant.now()) + ".csv";
                        // 兼容中文文件名
                        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                        return ok()
                            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                            .header("Cache-Control", "no-cache")
                            .bodyValue(csv);
                    });
            });
    }

    private List<Cooperation> applyFilters(List<Cooperation> all, String keyword, String type,
        String handledParam, String startTime, String endTime) {
        String kwLower = keyword != null ? keyword.toLowerCase() : "";
        boolean hasKeyword = kwLower != null && !kwLower.isBlank();
        boolean hasType = type != null && !type.isBlank() && !"all".equalsIgnoreCase(type);
        boolean hasHandled = handledParam != null && !handledParam.isBlank() && !"all".equalsIgnoreCase(handledParam);
        Boolean handledFilter = null;
        if (hasHandled) {
            if ("true".equalsIgnoreCase(handledParam) || "1".equals(handledParam)) {
                handledFilter = true;
            } else if ("false".equalsIgnoreCase(handledParam) || "0".equals(handledParam)) {
                handledFilter = false;
            } else {
                // 非法 handled 值视为不过滤，但前端不应传入
                hasHandled = false;
            }
        }
        Instant startInstant = parseInstant(startTime, false);
        Instant endInstant = parseInstant(endTime, true);

        List<Cooperation> result = new ArrayList<>();
        for (Cooperation c : all) {
            Cooperation.CooperationSpec spec = c.getSpec();
            if (spec == null) continue;

            // keyword 过滤 company/contact/phone (不区分大小写，前端已小写)
            if (hasKeyword) {
                String company = spec.getCompany() != null ? spec.getCompany().toLowerCase() : "";
                String contact = spec.getContact() != null ? spec.getContact().toLowerCase() : "";
                String phone = spec.getPhone() != null ? spec.getPhone().toLowerCase() : "";
                if (!company.contains(kwLower) && !contact.contains(kwLower) && !phone.contains(kwLower)) {
                    continue;
                }
            }
            // type 过滤
            if (hasType && !type.equals(spec.getType())) {
                continue;
            }
            // handled 过滤 null 视为 false
            if (hasHandled) {
                boolean isHandled = Boolean.TRUE.equals(spec.getHandled());
                if (isHandled != handledFilter) {
                    continue;
                }
            }
            // 时间区间过滤，按 creationTimestamp
            if (startInstant != null || endInstant != null) {
                Instant creation = null;
                if (c.getMetadata() != null) {
                    creation = c.getMetadata().getCreationTimestamp();
                }
                if (creation == null) {
                    // 无时间戳时若有时间过滤则排除
                    continue;
                }
                if (startInstant != null && creation.isBefore(startInstant)) {
                    continue;
                }
                if (endInstant != null && creation.isAfter(endInstant)) {
                    continue;
                }
            }
            result.add(c);
        }
        return result;
    }

    private String generateCsv(List<Cooperation> list) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM
        sb.append('\uFEFF');
        // 表头：与 PRD/Design 对齐
        sb.append("公司名称,联系人,联系电话,合作类型,合作类型标签,合作意向说明,来源页面,UserAgent,客户端IP,前端提交时间,创建时间,处理状态\n");
        DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;
        for (Cooperation c : list) {
            Cooperation.CooperationSpec s = c.getSpec();
            String company = s != null ? s.getCompany() : "";
            String contact = s != null ? s.getContact() : "";
            String phone = s != null ? s.getPhone() : "";
            String type = s != null ? s.getType() : "";
            String typeLabel = s != null ? s.getTypeLabel() : "";
            String message = s != null ? s.getMessage() : "";
            String sourceUrl = s != null ? s.getSourceUrl() : "";
            String userAgent = s != null ? s.getUserAgent() : "";
            String ip = s != null ? s.getIp() : "";
            String timestamp = s != null ? s.getTimestamp() : "";
            String creationTimestamp = "";
            if (c.getMetadata() != null && c.getMetadata().getCreationTimestamp() != null) {
                creationTimestamp = iso.format(c.getMetadata().getCreationTimestamp());
            }
            String handledLabel = (s != null && Boolean.TRUE.equals(s.getHandled())) ? "已处理" : "未处理";
            sb.append(escapeCsv(company)).append(',')
                .append(escapeCsv(contact)).append(',')
                .append(escapeCsv(phone)).append(',')
                .append(escapeCsv(type)).append(',')
                .append(escapeCsv(typeLabel)).append(',')
                .append(escapeCsv(message)).append(',')
                .append(escapeCsv(sourceUrl)).append(',')
                .append(escapeCsv(userAgent)).append(',')
                .append(escapeCsv(ip)).append(',')
                .append(escapeCsv(timestamp)).append(',')
                .append(escapeCsv(creationTimestamp)).append(',')
                .append(escapeCsv(handledLabel))
                .append('\n');
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        if (needQuote) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private Instant parseInstant(String s, boolean isEnd) {
        if (s == null || s.isBlank()) return null;
        String trimmed = s.trim();
        // 尝试 ISO_INSTANT (e.g. 2026-09-03T00:00:00Z)
        try {
            return Instant.parse(trimmed);
        } catch (Exception ignored) {
        }
        // 尝试 LocalDate (yyyy-MM-dd)
        try {
            LocalDate d = LocalDate.parse(trimmed);
            if (isEnd) {
                // 结束日期包含当天全天，设为 23:59:59.999999999
                return d.atTime(23, 59, 59, 999_999_999).atZone(ZoneOffset.UTC).toInstant();
            }
            return d.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception ignored) {
        }
        // 尝试 LocalDateTime (yyyy-MM-ddTHH:mm:ss)
        try {
            LocalDateTime dt = LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return dt.toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        // 尝试带空格的 datetime
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dt = LocalDateTime.parse(trimmed, fmt);
            return dt.toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        return null;
    }

    private enum AuthResult {
        ADMIN,
        UNAUTHORIZED,
        FORBIDDEN;

        Mono<ServerResponse> toResponse() {
            if (this == UNAUTHORIZED) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(CooperationResponse.fail("未授权，请先登录"));
            } else if (this == FORBIDDEN) {
                return ServerResponse.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(CooperationResponse.fail("无权限，需要管理员角色"));
            }
            return Mono.empty();
        }
    }

    private Mono<AuthResult> checkAdmin() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(auth -> {
                if (auth == null || !auth.isAuthenticated()) {
                    return AuthResult.UNAUTHORIZED;
                }
                Object principal = auth.getPrincipal();
                if (principal != null && "anonymousUser".equalsIgnoreCase(String.valueOf(principal))) {
                    return AuthResult.UNAUTHORIZED;
                }
                // 调试日志：打印当前用户权限，便于排查 Halo 实际角色名
                log.debug("checkAdmin user={}, authorities={}", principal, auth.getAuthorities());
                // 兼容 Halo 不同版本的管理员角色命名：大小写不敏感，且包含 admin/super/root 均视为管理员
                boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> {
                    String authority = a.getAuthority();
                    if (authority == null) return false;
                    String lower = authority.toLowerCase();
                    return lower.contains("admin") || lower.contains("super") || lower.contains("root");
                });
                // 若没有任何 authorities（如 Halo 早期版本直接通过 isAuthenticated 判断），则已认证即视为管理员，避免误拦截超级管理员
                if (!isAdmin) {
                    // 若用户已认证但无任何角色，可能是 Halo 的超级管理员（默认拥有全部权限），放行并记录警告
                    if (auth.getAuthorities().isEmpty()) {
                        log.warn("checkAdmin 放行无角色的已认证用户: {}", principal);
                        return AuthResult.ADMIN;
                    }
                    return AuthResult.FORBIDDEN;
                }
                return AuthResult.ADMIN;
            })
            .defaultIfEmpty(AuthResult.UNAUTHORIZED)
            .onErrorReturn(AuthResult.UNAUTHORIZED);
    }

    private String validate(CooperationRequest req) {
        String company = req.getCompany() != null ? req.getCompany().trim() : "";
        if (company.isEmpty()) return "公司名称为必填";
        if (company.length() < 2) return "公司名称至少 2 个字符";
        if (company.length() > 50) return "公司名称不能超过 50 个字符";

        String contact = req.getContact() != null ? req.getContact().trim() : "";
        if (contact.isEmpty()) return "联系人为必填";
        if (contact.length() < 2) return "联系人至少 2 个字符";
        if (contact.length() > 20) return "联系人不能超过 20 个字符";

        String phone = req.getPhone() != null ? req.getPhone().trim() : "";
        if (phone.isEmpty()) return "联系电话为必填";
        if (!phone.matches(PHONE_RE)) return "联系电话格式不正确";

        String type = req.getType() != null ? req.getType().trim() : "";
        if (type.isEmpty()) return "合作类型为必填";
        if (!ALLOWED_TYPES.contains(type)) return "合作类型不合法";

        String message = req.getMessage() != null ? req.getMessage().trim() : "";
        if (message.length() > 500) return "合作意向说明不能超过 500 个字符";

        return null;
    }

    private String getClientIp(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip.trim();
        if (exchange.getRequest().getRemoteAddress() != null
            && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
