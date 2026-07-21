package com.company.assistant.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.company.assistant.auth.JwtAuthFilter;
import com.company.assistant.auth.RestAccessDeniedHandler;
import com.company.assistant.auth.RestAuthenticationEntryPoint;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

public SecurityConfig(JwtAuthFilter jwtAuthFilter, RestAuthenticationEntryPoint restAuthenticationEntryPoint, RestAccessDeniedHandler restAccessDeniedHandler) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    this.restAccessDeniedHandler = restAccessDeniedHandler;
}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/health",
                    "/auth/login",
                    "/auth/2fa/verify",
                    "/auth/refresh",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(restAuthenticationEntryPoint)
            .accessDeniedHandler(restAccessDeniedHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}