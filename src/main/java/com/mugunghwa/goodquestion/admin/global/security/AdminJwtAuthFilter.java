package com.mugunghwa.goodquestion.admin.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Authorization 헤더의 관리자 토큰을 검증해 SecurityContext에 심는다. */
@Component
@RequiredArgsConstructor
public class AdminJwtAuthFilter extends OncePerRequestFilter {

    private final AdminJwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AdminPrincipal admin = jwtProvider.verify(header.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(
                        admin, null, List.of(new SimpleGrantedAuthority(admin.role().authority())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // 만료·위조 → 인증 없음 → 401. 여기서 응답을 쓰지 않는 이유는
                // 인증 없이 열어 둔 경로(헬스체크)가 헤더를 잘못 달고 와도 통과해야 하기 때문이다.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
