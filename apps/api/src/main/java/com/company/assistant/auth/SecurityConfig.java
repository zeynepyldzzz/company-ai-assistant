package com.company.assistant.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * GEÇİCİ AYAR: Projede henüz gerçek login/RBAC sistemi kurulmadığı için
 * şimdilik tüm HTTP isteklerine izin veriyoruz (permitAll). Böylece C-1'deki
 * eski endpoint'ler (menü, vs.) yine şifresiz çalışır.
 *
 * @EnableMethodSecurity, @PreAuthorize("hasRole('ADMIN')") gibi etiketlerin
 * çalışmasını sağlıyor. Gerçek login sistemi kurulunca (kullanıcıya rol
 * atayan bir mekanizma), /admin/** endpoint'leri otomatik olarak sadece
 * ADMIN rolündeki kullanıcılara açılacak. Şu an login sistemi olmadığı için
 * herkes "anonim" sayılıyor ve ADMIN rolü kimsede yok -> /admin/** istekleri
 * 403 Forbidden dönecek. Bu NORMAL ve GÜVENLİ bir davranış.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}