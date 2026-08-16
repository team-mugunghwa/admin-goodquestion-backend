package com.mugunghwa.goodquestion.admin.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 예약 작업을 켠다. 지금은 공지 예약 공개 하나가 쓴다.
 *
 * <p>애플리케이션 클래스에 붙이지 않고 따로 둔 것은, 테스트가 슬라이스로 뜰 때
 * 스케줄러까지 따라 뜨는 것을 눈에 보이게 하려는 것이다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
