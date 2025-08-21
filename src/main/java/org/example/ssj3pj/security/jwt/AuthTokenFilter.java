package org.example.ssj3pj.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.services.CustomUserDetailsService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpMethod;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils; // 수정해 둔 JwtUtils (createAccess/parse 등 보유)
    private final CustomUserDetailsService userDetailsService;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /** 로그인/리프레시/로그아웃 등은 Access 헤더 없이도 열려 있어야 하므로 필터 건너뜀 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 필요시 swagger, health 등도 추가
        return PATH_MATCHER.match("/api/auth/**", uri)
                || PATH_MATCHER.match("/v3/api-docs/**", uri)
                || PATH_MATCHER.match("/swagger-ui/**", uri)
                || PATH_MATCHER.match("/swagger-ui.html", uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(headerAuth) || !headerAuth.startsWith("Bearer ")) {
            // 헤더가 없으면 인증 없이 다음 필터로
            chain.doFilter(request, response);
            return;
        }

        String token = headerAuth.substring(7);

        try {
            // 유효성/서명/만료 검증
            var jws = jwtUtils.parse(token);
            String username = jws.getBody().getSubject();

            // 필요 시 role 등을 jws.getBody().get("role") 로 읽어 커스텀 권한 구성 가능
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.debug("Access token expired: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 프론트가 /api/auth/refresh 호출
        } catch (JwtException e) {
            log.debug("Invalid access token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
