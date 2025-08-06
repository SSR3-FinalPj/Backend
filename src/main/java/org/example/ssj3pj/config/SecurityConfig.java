package org.example.ssj3pj.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화 (POST 테스트 가능하게)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // 로그인/회원가입 허용
                        .anyRequest().authenticated()                 // 나머지는 인증 필요
                )
                .httpBasic(Customizer.withDefaults()); // (선택) HTTP 기본 로그인 방식 허용

        return http.build();
    }
}
