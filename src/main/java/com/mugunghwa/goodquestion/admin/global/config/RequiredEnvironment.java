package com.mugunghwa.goodquestion.admin.global.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * 기동에 반드시 필요한 환경변수를 검사한다.
 *
 * <h2>왜 따로 검사하나</h2>
 *
 * 값이 빠지면 앱은 어차피 죽는다. 다만 그때 나오는 문장이 원인을 가린다.
 * {@code DB_URL} 을 아예 넣지 않아도 스프링은 {@code 'url' must start with "jdbc"}
 * 라고 말한다. 형식이 틀린 것처럼 들리지만 실제로는 변수가 없는 것이다.
 * {@code ADMIN_JWT_SECRET} 이 짧으면 암호화 라이브러리의 영문 예외가 스택트레이스
 * 한가운데에서 나온다. 배포 로그를 처음 보는 사람이 읽어 낼 수 있는 문장이 아니다.
 *
 * <h2>무엇을 검사하고 무엇을 넘기나</h2>
 *
 * 기본값이 없는 둘({@code DB_URL}, {@code ADMIN_JWT_SECRET})만 막는다.
 * {@code DB_USERNAME} 과 {@code DB_PASSWORD} 는 비워 두는 로컬 설정(trust 인증)이
 * 실제로 있어서 빈 값을 오류로 보지 않는다. 값이 틀린 경우는 드라이버가
 * "password authentication failed" 라고 이미 분명히 말해 준다.
 *
 * <p>순수 함수로 두어 스프링 없이 테스트한다.
 */
public final class RequiredEnvironment {

    /** HS256 서명에 필요한 최소 길이. 이보다 짧으면 라이브러리가 키를 거부한다. */
    static final int MIN_SECRET_BYTES = 32;

    /** Railway 가 다른 서비스의 값을 끌어 쓸 때의 표기. 남아 있으면 참조가 풀리지 않은 것이다. */
    private static final String UNRESOLVED_REFERENCE = "${{";

    private RequiredEnvironment() {
    }

    /**
     * 문제를 찾아 사람이 읽을 문장으로 돌려준다.
     *
     * @param lookup 변수 이름으로 값을 찾는 함수. 없으면 null 을 준다
     * @return 문제 목록. 비어 있으면 이상 없음
     */
    public static List<String> check(UnaryOperator<String> lookup) {
        List<String> problems = new ArrayList<>();
        checkDatabaseUrl(lookup.apply("DB_URL"), problems);
        checkJwtSecret(lookup.apply("ADMIN_JWT_SECRET"), problems);
        return problems;
    }

    private static void checkDatabaseUrl(String url, List<String> problems) {
        if (isBlank(url)) {
            problems.add("""
                    DB_URL 이 비어 있습니다.
                      서비스 백엔드와 같은 DB 를 가리켜야 합니다. jdbc: 로 시작해야 합니다.
                      예) jdbc:postgresql://호스트:5432/goodquestion""");
            return;
        }
        if (url.contains(UNRESOLVED_REFERENCE)) {
            problems.add("""
                    DB_URL 의 Railway 변수 참조가 풀리지 않았습니다: %s
                      ${{ Postgres.PGHOST }} 같은 참조는 같은 프로젝트 안에 그 서비스가
                      있어야 값으로 바뀝니다. 관리자 백엔드를 새 프로젝트에 만들었다면
                      기존 goodquestion-backend 와 Postgres 가 있는 프로젝트로 옮기세요.""".formatted(url));
            return;
        }
        if (!url.startsWith("jdbc:")) {
            problems.add("""
                    DB_URL 이 jdbc: 로 시작하지 않습니다: %s
                      Railway 의 Postgres 가 주는 DATABASE_URL 은 postgresql:// 로 시작해서
                      그대로 붙여 넣으면 스프링이 읽지 못합니다. 아래처럼 직접 조립하세요.
                      jdbc:postgresql://${{ Postgres.PGHOST }}:${{ Postgres.PGPORT }}/${{ Postgres.PGDATABASE }}"""
                    .formatted(url));
        }
    }

    private static void checkJwtSecret(String secret, List<String> problems) {
        if (isBlank(secret)) {
            problems.add("""
                    ADMIN_JWT_SECRET 이 비어 있습니다.
                      관리자 토큰 서명 키입니다. openssl rand -base64 32 로 만드세요.
                      서비스 백엔드의 JWT_SECRET 과 반드시 다른 값을 씁니다.
                      같으면 보호자 앱이 받은 토큰으로 관리자 API 가 열립니다.""");
            return;
        }
        if (secret.contains(UNRESOLVED_REFERENCE)) {
            problems.add("""
                    ADMIN_JWT_SECRET 의 변수 참조가 풀리지 않았습니다: %s""".formatted(secret));
            return;
        }
        int length = secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            problems.add("""
                    ADMIN_JWT_SECRET 이 너무 짧습니다: %d바이트 (최소 %d바이트)
                      서명 방식이 HS256 이라 그보다 짧은 키는 거부됩니다.
                      openssl rand -base64 32 로 다시 만드세요."""
                    .formatted(length, MIN_SECRET_BYTES));
        }
    }

    /** 배포 로그에서 눈에 띄도록 문제들을 한 덩어리로 묶는다. */
    public static String describe(List<String> problems) {
        StringBuilder message = new StringBuilder();
        message.append("\n")
                .append("=".repeat(70)).append("\n")
                .append("관리자 백엔드를 시작할 수 없습니다. 환경변수를 확인하세요.\n")
                .append("=".repeat(70)).append("\n\n");
        for (String problem : problems) {
            message.append("- ").append(problem).append("\n\n");
        }
        message.append("""
                로컬이라면 .env.example 을 .env 로 복사해 값을 채웁니다.
                Railway 라면 서비스 -> Variables 탭에 넣습니다.
                자세한 절차는 docs/deploy-railway.md 에 있습니다.
                """);
        message.append("=".repeat(70)).append("\n");
        return message.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
