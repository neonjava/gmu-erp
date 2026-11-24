package in.neonjava.erp.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    /** Calls from the React app (fetch) get JSON and status codes instead of redirects. */
    static boolean isApi(HttpServletRequest req) {
        return req.getRequestURI().startsWith(req.getContextPath() + "/api/")
                || "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }

    private static void json(HttpServletResponse res, int status, String body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write(body);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LoginAttemptService attempts) throws Exception {
        http
            // CSRF token in a cookie the React app can read and send back as the X-XSRF-TOKEN header.
            // Thymeleaf forms keep sending it as the _csrf field.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/app", "/app/**", "/css/**", "/js/**", "/img/**", "/error").permitAll()
                .requestMatchers("/api/me").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/faculty/**", "/api/faculty/**").hasRole("FACULTY")
                .requestMatchers("/student/**", "/api/student/**").hasRole("STUDENT")
                .requestMatchers("/events/new").hasAnyRole("ADMIN", "FACULTY")
                .requestMatchers(HttpMethod.POST, "/events").hasAnyRole("ADMIN", "FACULTY")
                .requestMatchers(HttpMethod.POST, "/events/*/register", "/events/*/cancel").hasRole("STUDENT")
                .anyRequest().authenticated())
            // API calls get 401; a browser opening a page while signed out goes to the login screen.
            .exceptionHandling(e -> e
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), SecurityConfig::isApi)
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/app/login"), req -> !isApi(req)))
            .formLogin(form -> form
                .loginPage("/app/login")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> {
                    attempts.succeeded(auth.getName());
                    if (isApi(req)) json(res, 200, "{\"ok\":true}");
                    else res.sendRedirect(req.getContextPath() + "/app/");
                })
                // Tell the person why: wrong password (and tries left), locked, or turned off.
                .failureHandler((req, res, ex) -> {
                    String user = req.getParameter("username");
                    String reason;
                    int left = 0;
                    if (ex instanceof LockedException) reason = "locked";
                    else if (ex instanceof DisabledException) reason = "disabled";
                    else if (attempts.failed(user)) reason = "locked";
                    else { reason = "bad"; left = attempts.triesLeft(user); }
                    if (isApi(req)) json(res, 401, "{\"reason\":\"" + reason + "\",\"left\":" + left + "}");
                    else res.sendRedirect(req.getContextPath() + "/app/login?error=" + reason);
                })
                .permitAll())
            .rememberMe(rm -> rm.key("gmu-erp-remember-me").tokenValiditySeconds(7 * 24 * 3600))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((req, res, auth) -> {
                    if (isApi(req)) res.setStatus(204);
                    else res.sendRedirect(req.getContextPath() + "/app/login?logout");
                })
                .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
