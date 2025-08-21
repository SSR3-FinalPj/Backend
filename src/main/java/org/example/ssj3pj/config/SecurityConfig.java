package org.example.ssj3pj.config;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.security.jwt.AuthTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;

    // ✅ AuthTokenFilter가 서블릿 필터로 자동 등록되는 걸 확실히 막음 (보안 체인 외부에서 403/401 줄 가능성 차단)
    @Bean
    public FilterRegistrationBean<AuthTokenFilter> disableAuthFilterRegistration(AuthTokenFilter f) {
        FilterRegistrationBean<AuthTokenFilter> frb = new FilterRegistrationBean<>(f);
        frb.setEnabled(false);
        return frb;
    }

    @Bean
    public SecurityFilterChain openAll(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());  // 🔓 전부 허용
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
