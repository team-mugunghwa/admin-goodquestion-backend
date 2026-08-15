package com.mugunghwa.goodquestion.admin.global.config;

import com.mugunghwa.goodquestion.admin.global.security.CurrentAdminArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentAdminArgumentResolver currentAdminArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAdminArgumentResolver);
    }

    // CORS는 여기가 아니라 CorsConfig에 있다. 이유는 그 파일의 주석에 있다.
}
