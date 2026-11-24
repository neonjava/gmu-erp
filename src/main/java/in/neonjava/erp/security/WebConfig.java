package in.neonjava.erp.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccountGuard guard;

    public WebConfig(AccountGuard guard) {
        this.guard = guard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(guard).excludePathPatterns(
                "/login", "/logout", "/error", "/css/**", "/js/**", "/img/**", "/app", "/app/**", "/api/me", "/api/me/password");
    }
}
