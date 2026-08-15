package com.mugunghwa.goodquestion.admin.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 관리자 콘솔(Flutter Web)에서 오는 교차 출처 요청 설정.
 *
 * <p>이 빈은 SecurityConfig의 {@code .cors()}가 찾아 쓴다. {@code WebMvcConfigurer.addCorsMappings}로
 * 두면 안 된다 - 시큐리티 필터 체인이 DispatcherServlet보다 먼저 돌아서, Authorization
 * 헤더가 없는 preflight(OPTIONS)가 MVC까지 가지 못하고 401로 잘린다. curl은 되는데
 * 브라우저에서만 전부 실패하는 형태로 나타난다.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOriginPatterns;

    public CorsConfig(@Value("${cors.allowed-origin-patterns}") List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // 쿠키를 쓰지 않는다. 토큰은 Authorization 헤더로 보낸다.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
