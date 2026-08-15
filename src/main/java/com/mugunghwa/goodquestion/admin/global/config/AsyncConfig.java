package com.mugunghwa.goodquestion.admin.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * {@code @Async}를 켠다.
 *
 * <p>실행기를 따로 만들지 않는다. {@code spring.threads.virtual.enabled=true}라
 * 부트가 만드는 기본 실행기가 요청마다 가상 스레드를 띄운다. 푸시 발송은 대부분의
 * 시간을 벤더 응답 대기로 보내므로 플랫폼 스레드를 쥐고 있을 이유가 없다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
