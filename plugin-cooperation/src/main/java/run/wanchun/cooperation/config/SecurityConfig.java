package run.wanchun.cooperation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * 放行合作表单 POST 匿名访问，其余 GET 需鉴权。
 * Halo 主链已存在，此链以更高优先级匹配合作接口。
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityWebFilterChain cooperationSecurityFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/apis/api.cooperation.vivid.run/**"))
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.POST, "/apis/api.cooperation.vivid.run/v1alpha1/cooperations")
                .permitAll()
                .anyExchange().authenticated()
            )
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
