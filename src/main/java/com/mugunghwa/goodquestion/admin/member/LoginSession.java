package com.mugunghwa.goodquestion.admin.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 사용자의 로그인 세션. 서비스의 {@code refresh_tokens} 테이블을 가리킨다.
 *
 * <p>이름을 RefreshToken이 아니라 LoginSession으로 둔 것은 관리자 화면에서의 뜻 때문이다.
 * 관리자가 여기서 하는 일은 "이 기기의 로그인을 끊는다"이지 토큰을 다루는 것이 아니다.
 *
 * <p>토큰 해시는 매핑하지 않는다. 화면에 보여줄 이유가 없고, 매핑해 두면 응답에 실릴
 * 길이 생긴다. 액세스 토큰은 짧게(30분) 살아 있어서 여기서 끊어도 그만큼은 남는다 -
 * 즉시 차단이 필요하면 계정 정지를 함께 쓴다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    void revoke() {
        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now();
        }
    }
}
