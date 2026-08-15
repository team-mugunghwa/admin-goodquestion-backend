package com.mugunghwa.goodquestion.admin.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 스프링 컨텍스트가 필요한 테스트에 붙인다.
 *
 * <p>세 요소를 한 애노테이션으로 묶어 두면 테스트마다 조합이 갈려 컨텍스트 캐시가
 * 쪼개지는 일을 막을 수 있다. 컨테이너는 전체 실행에서 한 번만 뜬다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresContainerConfig.class, AdminFixture.class})
public @interface IntegrationTest {
}
