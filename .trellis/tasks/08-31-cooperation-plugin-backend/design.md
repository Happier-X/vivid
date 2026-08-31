# 技术设计：自建合作表单 Halo 插件后端

## 架构总览

```
Browser (page_cooperation.html)
  ↓ POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations (同源)
Halo (plugin-cooperation.jar 同进程)
  ↓ RouterFunction → CooperationController
  ├─ 校验 → 限流(Memory) → Extension 存储 → MailSender
  └─ ReactiveExtensionClient (H2/Postgres)
Halo Console (插件设置: SMTP/限流)
```

同源无 CORS，插件热插拔，数据落 Halo Extension。

## 工程结构

```
plugin-cooperation/
├── build.gradle                // halo-plugin + spring-mail + halo-extension
├── settings.gradle
├── src/main/java/run/wanchun/cooperation/
│   ├── CooperationPlugin.java          // extends BasePlugin
│   ├── config/CooperationProperties.java // @ConfigurationProperties 映射 plugin.yaml setting
│   ├── extension/Cooperation.java      // @GVK Group api.cooperation.vivid.run
│   ├── dto/CooperationRequest.java     // Jakarta Validation
│   ├── controller/CooperationController.java // RouterFunction 或 @RestController
│   ├── service/CooperationService.java // 校验+限流+落库
│   ├── service/EmailService.java       // JavaMailSender
│   └── service/RateLimiter.java        // ConcurrentHashMap
├── src/main/resources/
│   ├── plugin.yaml                 // 插件元数据 + settingName
│   └── extensions/cooperation.yaml // CRD 定义 (可选，Halo 自动注册)
└── README.md
```

### build.gradle 要点

- `id "run.halo.tools.plugin" version "0.8.x"`，`halo.version >=2.20.0`
- 依赖：`spring-boot-starter-mail`、`halo-common`、`halo-application`（provided）、`lombok` 可选
- `halo {}` 配置 `pluginVersion`、`requires`

### plugin.yaml

```yaml
apiVersion: plugin.halo.run/v1alpha1
kind: Plugin
metadata:
  name: cooperation-plugin
spec:
  displayName: 合作咨询插件
  version: 1.0.0
  author: 山东万椿
  requires: ">=2.20.0"
  settingName: cooperation-setting
  configMapName: cooperation-config
```

### settings.yaml（插件设置）

```yaml
apiVersion: v1alpha1
kind: Setting
metadata:
  name: cooperation-setting
spec:
  forms:
    - group: mail
      label: 邮件配置
      formSchema:
        - $formkit: text
          name: smtpHost
          label: SMTP Host
        - $formkit: text
          name: smtpPort
          label: SMTP Port
          value: "465"
        - $formkit: text
          name: smtpUsername
          label: SMTP 用户
        - $formkit: password
          name: smtpPassword
          label: SMTP 密码
        - $formkit: text
          name: fromEmail
          label: 发件人
          value: "noreply@wanchunsmart.com"
        - $formkit: text
          name: receiverEmail
          label: 收件人
          value: "contact@wanchunsmart.com"
    - group: security
      label: 安全
      formSchema:
        - $formkit: number
          name: rateLimitSeconds
          label: 限流秒数
          value: 60
```

## API 设计

### POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations

- 匿名 `permitAll()`，`@RequestBody CooperationRequest`
- 校验：`@NotBlank @Size 2-50` 等，`@Pattern` 复用 `PHONE_RE`
- 流程：
  1. 蜜罐 `website` 非空 → `200 {success:true}`
  2. `validate(request)` → `400`
  3. `rateLimiter.tryAcquire(ip)` → `429`
  4. `extensionClient.create(cooperation)` → 持久化
  5. `emailService.send(cooperation)` → 失败 `500`（记录仍保留）
  6. 返回 `200 {success:true}`

### GET /apis/api.cooperation.vivid.run/v1alpha1/cooperations

- 需 `hasRole("ADMIN")`，`@RequestParam page/size`
- `extensionClient.list(Cooperation.class, ...)` → `ListResult`

### Extension 定义

```java
@GVK(group="api.cooperation.vivid.run", version="v1alpha1", kind="Cooperation", plural="cooperations", singular="cooperation")
public class Cooperation extends AbstractExtension {
  @Schema
  public static class CooperationSpec {
    String company; String contact; String phone; String type; String typeLabel;
    String message; String website; String sourceUrl; String userAgent; String timestamp; String ip;
  }
}
```

## 限流实现

```java
@Component
public class RateLimiter {
  private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();
  public boolean tryAcquire(String ip, int seconds) { /* compareAndSet */ }
}
```

定期清理过期 key（`@Scheduled` 或每次写入时清理）。

## 邮件实现

- `JavaMailSenderImpl` 动态配置：从 `CooperationProperties` 读 SMTP，`setHost/Port/Username/Password`，`setJavaMailProperties`（`mail.smtp.auth/ssl`）
- `MimeMessageHelper` 组装标题/正文，`setFrom(fromEmail)`，`setTo(receiverEmail)`

## 主题联动

- `vivid/settings.yaml` 中 `cooperation.endpoint` 默认值改为 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`
- 前端仍通过 `data-endpoint` 注入，同源请求无需 CORS 白名单

## 构建与部署

- `./gradlew :plugin-cooperation:build -x test` → `plugin-cooperation/build/libs/cooperation-plugin-1.0.0.jar`
- 拷贝到 Halo `plugins/` 目录，重启 Halo，控制台启用插件并配置 SMTP
- 前端主题设置填同源路径，无需外网

## 兼容与降级

- 未配置 SMTP 时，仍落库但跳过发信并日志告警
- 插件未启用时，前端提交返回 `404`，前端显示“提交失败，请稍后重试或电话联系”

## 风险与回滚

- 风险：SMTP 配置错误导致 500 → 文档说明测试发信步骤
- 回滚：停用插件并将主题 `endpoint` 切回空或外部 Worker 即回退
