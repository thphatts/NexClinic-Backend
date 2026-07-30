package com.thphatts.clinicportal.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // tắt csrf vì dùng jwt qua header (stateless) không cần lưu cookie/session để xác thực
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // STATELESS = sever không tạo và lưu session cho user. mỗi req phải chứng minh danh tính qua token và sever không nhớ req trước đó
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/error",
                                "/health",
                                "/actuator/health",
                                "/api/v1/auth/**",
                                "/api/v1/ai/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/configuration/ui",
                                "/configuration/security",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // CHÚ THÍCH VỀ COOKIE & CORS:
        // 1. Spring MVC tự động đọc Cookie thông qua annotation @CookieValue độc lập với Security filter chain.
        // 2. Tuy nhiên, khi gửi Cookie/Credentials từ Frontend (Cross-Origin), trình duyệt yêu cầu:
        //    - setAllowCredentials(true): Cho phép gửi/nhận cookie và header xác thực.
        //    - setAllowedOriginPatterns: Phải chỉ định rõ domain của Frontend (ví dụ: http://localhost:3000, http://localhost:5173).
        //    - TRÌNH DUYỆT SẼ TỪ CHỐI GỬI COOKIE NẾU ORIGIN LÀ WILDCARD "*" KHI KẾT HỢP VỚI ALLOW CREDENTIALS!
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
