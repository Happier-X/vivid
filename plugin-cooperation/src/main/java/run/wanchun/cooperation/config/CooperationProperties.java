package run.wanchun.cooperation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 插件设置映射，Halo 会将 ConfigMap（cooperation-config）数据绑定到此处。
 * 若 Halo 版本不支持自动绑定，则回退为 Environment 手动读取。
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin.cooperation")
public class CooperationProperties {

    /** SMTP Host */
    private String smtpHost = "";
    /** SMTP Port */
    private Integer smtpPort = 465;
    /** SMTP 用户名 */
    private String smtpUsername = "";
    /** SMTP 密码/授权码 */
    private String smtpPassword = "";
    /** 发件人邮箱 */
    private String fromEmail = "noreply@wanchunsmart.com";
    /** 收件人邮箱 */
    private String receiverEmail = "contact@wanchunsmart.com";
    /** 是否启用 SSL，465 为 true，587 为 false */
    private Boolean smtpSsl = true;
    /** IP 限流秒数 */
    private Integer rateLimitSeconds = 60;
}
