package org.example.ssj3pj.config;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;
    @Bean
    public FilterRegistrationBean<AuthTokenFilter> disableAuthFilterRegistration(AuthTokenFilter f) {
        FilterRegistrationBean<AuthTokenFilter> frb = new FilterRegistrationBean<>(f);
        frb.setEnabled(false);
        return frb;
    }

    @Bean
    public SecurityFilterChain openAll(HttpSecurity http) throws Exception {
        http
                .httpBasic(b -> b.disable())
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()     // 프리플라이트 허용
                        .requestMatchers(
                                "/api/auth/login", "/api/auth/refresh", "/api/auth/logout","/api/notify/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html","/api/google/**",
                                "/api/dashboard/**", "/api/upload/*","/api/images/*","/api/youtube/**",
                                "/api/reddit/**","/api/videos/**     "
                        ).permitAll()
                        .requestMatchers("/api/youtube/channelId").permitAll()   // login-url, callback 등 전부 개방

                        .anyRequest().authenticated()
                )
                .formLogin(f -> f.disable())
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class); // ← 여기
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true); // 쿠키/인증 정보 전송 허용
        cfg.setAllowedOrigins(List.of(allowedOrigins));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        // (선택) 브라우저에서 필요한 응답 헤더 노출
        cfg.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
