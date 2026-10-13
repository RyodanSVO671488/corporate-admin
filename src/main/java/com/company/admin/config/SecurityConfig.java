package com.company.admin.config;

import com.company.admin.security.*;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DatabaseUserDetailsService users) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/login", "/css/**", "/js/**", "/favicon.svg", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/").authenticated()
                .requestMatchers(HttpMethod.GET, "/users/new", "/users/*/edit").hasAuthority("USER_WRITE")
                .requestMatchers(HttpMethod.GET, "/users").hasAuthority("USER_READ")
                .requestMatchers(HttpMethod.POST, "/users/new", "/users/*").hasAuthority("USER_WRITE")
                .requestMatchers(HttpMethod.GET, "/roles/new", "/roles/*/edit").hasAuthority("ROLE_WRITE")
                .requestMatchers(HttpMethod.GET, "/roles").hasAuthority("ROLE_READ")
                .requestMatchers(HttpMethod.POST, "/roles/new", "/roles/*").hasAuthority("ROLE_WRITE")
                .requestMatchers(HttpMethod.GET, "/audit").hasAuthority("AUDIT_READ")
                .anyRequest().denyAll())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .exceptionHandling(errors -> errors.accessDeniedPage("/error/403"))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; style-src 'self'; img-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .addFilterAfter(new SessionAccessRefreshFilter(users), AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
