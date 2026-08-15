package com.mugunghwa.goodquestion.admin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 리프레시 토큰의 저장용 해시.
 *
 * <p>BCrypt를 쓰지 않는 이유가 있다. BCrypt는 같은 입력이라도 매번 다른 해시를 만들어서
 * "이 토큰이 DB에 있는가"를 인덱스로 찾을 수 없다 - 모든 행을 꺼내 하나씩 대조해야 한다.
 * 그리고 BCrypt의 값어치는 사람이 고른 짧은 비밀번호를 무차별 대입에서 지키는 데 있는데,
 * 여기 담기는 것은 우리가 만든 256비트 난수라 그 위협이 성립하지 않는다.
 */
final class AdminTokenHasher {

    private AdminTokenHasher() {
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 반드시 제공한다. 여기 오면 실행 환경이 깨진 것이다.
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
