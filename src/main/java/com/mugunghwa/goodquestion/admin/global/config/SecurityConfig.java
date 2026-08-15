package com.mugunghwa.goodquestion.admin.global.config;

import com.mugunghwa.goodquestion.admin.global.security.AdminJwtAuthFilter;
import com.mugunghwa.goodquestion.admin.global.security.RestAccessDeniedHandler;
import com.mugunghwa.goodquestion.admin.global.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 관리자 API 보안.
 *
 * <p>서비스 백엔드와 달리 <b>기본이 잠금</b>이다. 여는 경로는 헬스체크와 로그인/재발급
 * 셋뿐이고 나머지는 전부 인증을 요구한다. 관리자 콘솔에는 공개해도 되는 데이터가 없다 -
 * 새 컨트롤러를 추가할 때 여기를 고칠 일이 없어야 정상이다.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AdminJwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        // 토큰 발급 이전 단계. 로그아웃은 토큰이 있어야 하므로 여기 넣지 않는다.
                        .requestMatchers("/api/admin/auth/login", "/api/admin/auth/refresh").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)   // 미인증 → 401
                        .accessDeniedHandler(accessDeniedHandler))            // 권한 없음 → 403
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
