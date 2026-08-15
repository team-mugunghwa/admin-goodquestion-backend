package com.mugunghwa.goodquestion.admin.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 관리자 액세스 토큰(HS256)의 발급과 검증.
 *
 * <p>서명 키는 서비스 백엔드의 {@code jwt.secret}과 반드시 달라야 한다. 같은 키를 쓰면
 * 보호자 앱이 받은 토큰의 서명이 여기서도 통과하고, 남는 것은 sub가 관리자 id인지
 * 보호자 id인지의 차이뿐이라 관리자 API가 사실상 열린다.
 */
@Component
public class AdminJwtProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expirationMs;

    public AdminJwtProvider(@Value("${admin.jwt.secret}") String secret,
                            @Value("${admin.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String issue(AdminPrincipal admin) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(admin.id().toString())
                .claim(CLAIM_EMAIL, admin.email())
                .claim(CLAIM_NAME, admin.name())
                .claim(CLAIM_ROLE, admin.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public long getExpiresInSeconds() {
        return expirationMs / 1000;
    }

    /** @return 검증된 관리자 정보 — 실패 시 JwtException */
    public AdminPrincipal verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return new AdminPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_NAME, String.class),
                AdminRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }
}
