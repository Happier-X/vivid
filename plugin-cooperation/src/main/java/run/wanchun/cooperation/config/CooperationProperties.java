package run.wanchun.cooperation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 插件设置映射，Halo 会将 ConfigMap（cooperation-config）数据绑定到此处。
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin.cooperation")
public class CooperationProperties {

    /** IP 限流秒数 */
    private Integer rateLimitSeconds = 60;
}
