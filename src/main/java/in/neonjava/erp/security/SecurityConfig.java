package in.neonjava.erp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/faculty/**").hasRole("FACULTY")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/events/new").hasAnyRole("ADMIN", "FACULTY")
                .requestMatchers(HttpMethod.POST, "/events").hasAnyRole("ADMIN", "FACULTY")
                .requestMatchers(HttpMethod.POST, "/events/*/register", "/events/*/cancel").hasRole("STUDENT")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .rememberMe(rm -> rm.key("gmu-erp-remember-me").tokenValiditySeconds(7 * 24 * 3600))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
