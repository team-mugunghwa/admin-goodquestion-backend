package com.mugunghwa.goodquestion.admin.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 환경변수 안내.
 *
 * <p>스프링을 띄우지 않는다. 검사가 순수 함수라서 그럴 필요가 없고, 이 검사 자체는
 * 스프링이 뜨기 전에 도는 것이라 컨텍스트를 띄우면 오히려 실제와 멀어진다.
 */
class RequiredEnvironmentTest {

    private static final String VALID_URL = "jdbc:postgresql://localhost:5432/goodquestion";
    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    private List<String> check(Map<String, String> env) {
        return RequiredEnvironment.check(env::get);
    }

    @Test
    @DisplayName("값이 다 있으면 아무 말도 하지 않는다")
    void passesWhenAllPresent() {
        assertThat(check(Map.of(
                "DB_URL", VALID_URL,
                "ADMIN_JWT_SECRET", VALID_SECRET))).isEmpty();
    }

    @Test
    @DisplayName("DB_URL 이 없으면 형식이 아니라 '비어 있음'이라고 말한다")
    void reportsMissingDatabaseUrl() {
        // 스프링에 맡기면 변수를 아예 안 넣어도 'url must start with jdbc' 라고만
        // 나온다. 형식 문제로 오해하게 만드는 문장이라 여기서 갈라 준다.
        List<String> problems = check(Map.of("ADMIN_JWT_SECRET", VALID_SECRET));

        assertThat(problems).hasSize(1);
        assertThat(problems.getFirst())
                .contains("DB_URL 이 비어 있습니다")
                .contains("jdbc:postgresql://");
    }

    @Test
    @DisplayName("postgresql:// 을 그대로 넣으면 jdbc 로 조립하라고 알려 준다")
    void reportsNonJdbcUrl() {
        List<String> problems = check(Map.of(
                "DB_URL", "postgresql://host:5432/goodquestion",
                "ADMIN_JWT_SECRET", VALID_SECRET));

        assertThat(problems).hasSize(1);
        assertThat(problems.getFirst())
                .contains("jdbc: 로 시작하지 않습니다")
                .contains("DATABASE_URL");
    }

    @Test
    @DisplayName("Railway 변수 참조가 안 풀린 채로 들어오면 그것부터 짚는다")
    void reportsUnresolvedReference() {
        // 새 프로젝트에 서비스를 만들면 이 상태가 된다. 형식만 보면 jdbc 로
        // 시작하니 통과해 버리는데, 정작 접속은 안 된다.
        List<String> problems = check(Map.of(
                "DB_URL", "jdbc:postgresql://${{ Postgres.PGHOST }}:5432/goodquestion",
                "ADMIN_JWT_SECRET", VALID_SECRET));

        assertThat(problems).hasSize(1);
        assertThat(problems.getFirst())
                .contains("참조가 풀리지 않았습니다")
                .contains("같은 프로젝트");
    }

    @Test
    @DisplayName("서명 키가 없으면 만드는 방법까지 알려 준다")
    void reportsMissingSecret() {
        List<String> problems = check(Map.of("DB_URL", VALID_URL));

        assertThat(problems).hasSize(1);
        assertThat(problems.getFirst())
                .contains("ADMIN_JWT_SECRET 이 비어 있습니다")
                .contains("openssl rand -base64 32")
                // 같은 키를 쓰면 생기는 일까지 적어 둔다. 이 경고가 가장 중요하다.
                .contains("보호자");
    }

    @Test
    @DisplayName("서명 키가 짧으면 몇 바이트인지 알려 준다")
    void reportsShortSecret() {
        List<String> problems = check(Map.of(
                "DB_URL", VALID_URL,
                "ADMIN_JWT_SECRET", "tooshort"));

        assertThat(problems).hasSize(1);
        assertThat(problems.getFirst())
                .contains("8바이트")
                .contains("%d바이트".formatted(RequiredEnvironment.MIN_SECRET_BYTES));
    }

    @Test
    @DisplayName("공백만 있는 값은 없는 것으로 본다")
    void treatsBlankAsMissing() {
        List<String> problems = check(Map.of(
                "DB_URL", "   ",
                "ADMIN_JWT_SECRET", " "));

        assertThat(problems).hasSize(2);
    }

    @Test
    @DisplayName("DB 사용자와 비밀번호가 비어도 막지 않는다")
    void allowsBlankCredentials() {
        // trust 인증으로 붙는 로컬 설정이 실제로 있다. .env.example 도 비워 둔다.
        // 값이 틀린 경우는 드라이버가 이미 분명하게 말해 준다.
        assertThat(check(Map.of(
                "DB_URL", VALID_URL,
                "ADMIN_JWT_SECRET", VALID_SECRET,
                "DB_USERNAME", "",
                "DB_PASSWORD", ""))).isEmpty();
    }

    @Test
    @DisplayName("문제가 여러 개면 한 번에 다 보여 준다")
    void reportsEveryProblemAtOnce() {
        // 하나 고치고 다시 배포해서 또 실패하는 일을 줄인다.
        List<String> problems = check(Map.of());
        assertThat(problems).hasSize(2);

        String message = RequiredEnvironment.describe(problems);
        assertThat(message)
                .contains("관리자 백엔드를 시작할 수 없습니다")
                .contains("DB_URL")
                .contains("ADMIN_JWT_SECRET")
                .contains("docs/deploy-railway.md");
    }
}
