package run.wanchun.cooperation.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
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
 * 合作咨询路由：POST 匿名可访问，GET 需鉴权。
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
            .GET("/apis/api.cooperation.vivid.run/v1alpha1/cooperations", this::handleList)
            .GET("/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}", this::handleGet)
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
        return requireAdmin()
            .flatMap(isAdmin -> {
                if (!isAdmin) {
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.fail("未授权"));
                }
                int page = parseInt(request.queryParam("page").orElse("0"), 0);
                int size = parseInt(request.queryParam("size").orElse("20"), 20);
                return extensionClient.list(Cooperation.class, null, null, page, size)
                    .flatMap(result -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result));
            });
    }

    private Mono<ServerResponse> handleGet(ServerRequest request) {
        return requireAdmin()
            .flatMap(isAdmin -> {
                if (!isAdmin) {
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(CooperationResponse.fail("未授权"));
                }
                String name = request.pathVariable("name");
                return extensionClient.get(Cooperation.class, name)
                    .flatMap(coop -> ok().contentType(MediaType.APPLICATION_JSON).bodyValue(coop))
                    .switchIfEmpty(ServerResponse.notFound().build());
            });
    }

    private Mono<Boolean> requireAdmin() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .map(Authentication::getAuthorities)
            .map(auths -> auths.stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority())
            ))
            .defaultIfEmpty(false);
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
