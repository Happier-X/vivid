# 合作咨询插件（cooperation-plugin）

自建 Halo 插件后端，为 `page_cooperation.html` 原生表单提供同源提交接口，替代 Cloudflare Worker 参考实现。

## 功能

- `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 匿名提交
- 服务端校验（公司 2-50、联系人 2-20、手机号正则、枚举、message 0-500、蜜罐）
- IP 内存限流（默认 60s 1 次，可配 `rateLimitSeconds`）
- 蜜罐 `website` 非空静默成功，不落库不发邮件
- 通过 `ReactiveExtensionClient` 创建 `Cooperation` Extension 持久化
- 通过 `JavaMailSender` 发送通知邮件（Halo 控制台 → 插件设置配置 SMTP）
- `GET /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 需 `ROLE_ADMIN`，支持分页 `page/size`，详情 `GET /{name}`

## 目录结构

```
plugin-cooperation/
├── build.gradle
├── settings.gradle
├── src/main/java/run/wanchun/cooperation/
│   ├── CooperationPlugin.java
│   ├── config/CooperationProperties.java
│   ├── config/SecurityConfig.java
│   ├── extension/Cooperation.java
│   ├── dto/CooperationRequest.java
│   ├── dto/CooperationResponse.java
│   ├── service/RateLimiter.java
│   ├── service/EmailService.java
│   └── controller/CooperationController.java
├── src/main/resources/
│   ├── plugin.yaml
│   └── extensions/setting.yaml
└── docs/DEPLOY.md
```

## 构建

要求 JDK 17+、Gradle 8.x

```bash
# 根目录或插件目录
./gradlew :plugin-cooperation:build -x test
# 产物
plugin-cooperation/build/libs/cooperation-plugin-1.0.0.jar
```

Halo 2.20+ 兼容，基于 `run.halo.app.plugin.BasePlugin` + Spring Boot 3.x + RouterFunction。

## 安装

1. 拷贝 Jar 到 Halo 的 `plugins/` 目录：
   ```bash
   cp plugin-cooperation/build/libs/cooperation-plugin-1.0.0.jar /path/to/halo/plugins/
   ```
2. 重启 Halo（或等待自动热加载）
3. Halo 控制台 → 插件 → 合作咨询插件 → 启用 → 设置

## 插件设置（Halo 控制台 → 插件设置）

- `smtpHost` / `smtpPort` / `smtpUsername` / `smtpPassword` / `fromEmail` / `receiverEmail` / `smtpSsl` / `rateLimitSeconds`
- 修改后无需重启，配置通过 `CooperationProperties`（`plugin.cooperation.*`）注入
- 未配置 SMTP 时，仍落库但跳过发信并输出 `WARN` 日志

## 主题联动

- 主题 `settings.yaml` 中 `cooperation.endpoint` 默认值已改为同源路径：
  ` /apis/api.cooperation.vivid.run/v1alpha1/cooperations`
- 前端 `src/js/cooperation-form.ts` 支持相对路径（`/apis/...`）与绝对 `https://`，同源请求无 CORS
- 若插件未启用，前端提交将返回 404，前端展示“提交失败，请稍后重试或电话联系”

## 联调示例

```bash
# 成功提交
curl -X POST http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations \
  -H "Content-Type: application/json" \
  -d '{"company":"山东测试公司","contact":"张三","phone":"13812345678","type":"institution","typeLabel":"养老机构合作","message":"合作意向","website":"","sourceUrl":"https://example.com/cooperation","userAgent":"curl","timestamp":"2026-08-31T00:00:00Z"}'

# 非法 phone -> 400
curl -X POST ... -d '{"company":"AB","contact":"张三","phone":"123","type":"institution"}'

# 蜜罐 -> 200 静默成功
curl -X POST ... -d '{"company":"AB","contact":"张三","phone":"13812345678","type":"institution","website":"spam"}'

# 限流 -> 429（60s 内二次提交）

# 查询（需管理员 Cookie/Token）
curl http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations?page=0&size=20 \
  -H "Cookie: halo-SESSION=xxx"
```

## 排错

- `400` 检查服务端校验文案是否与前端一致
- `429` 等待 `rateLimitSeconds` 后重试，或在插件设置调小
- `500` 查看 Halo 日志 `CooperationController` / `EmailService`，常见为 SMTP 配置错误
- 限流为内存 `ConcurrentHashMap`，多实例部署退化为单机限流，如需分布式请接入 Redis（文档说明）

## 合规

- 无飞书相关代码与文档
- 插件遵循 Halo 插件规范（`plugin.yaml` + `BasePlugin`），不修改 Halo 核心
- 依赖 `run.halo.app:api:2.20.11`，兼容 `>=2.20.0`
