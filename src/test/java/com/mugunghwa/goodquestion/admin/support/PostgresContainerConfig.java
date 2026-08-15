package com.mugunghwa.goodquestion.admin.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 테스트용 PostgreSQL 컨테이너.
 *
 * <p>빈 DB에서 시작해 마이그레이션만으로 스키마를 만든다. 관리자 마이그레이션은 서비스
 * 테이블(parents 등)이 있는 상태를 전제하므로, {@code application-test.yml}이 Flyway
 * 경로에 {@code db/baseline}을 먼저 추가한다. 그 폴더가 "서비스 백엔드가 이미 돌아 있는
 * 상태"를 재현한다.
 *
 * <p>{@code ./gradlew test -PlocalDb}로 실행하면 이 설정이 꺼지고 .env의 DB에 붙는다.
 */
@TestConfiguration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "test.datasource.mode", havingValue = "testcontainers", matchIfMissing = true)
public class PostgresContainerConfig {

    /** 운영/로컬과 같은 메이저 버전. 버전이 갈리면 잡히지 않는 문법 차이가 생긴다. */
    private static final String IMAGE = "postgres:17";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(IMAGE);
    }
}
