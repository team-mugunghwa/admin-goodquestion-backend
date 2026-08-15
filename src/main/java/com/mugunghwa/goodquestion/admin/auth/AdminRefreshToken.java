package com.mugunghwa.goodquestion.admin.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 관리자 리프레시 토큰.
 *
 * <p>원문은 저장하지 않는다. DB가 유출돼도 그 값으로 재발급을 받을 수 없어야 한다.
 * 사용하면 즉시 폐기하고 새로 발급한다(회전) - 같은 토큰이 두 번 쓰이면 그중 하나는
 * 탈취된 것이므로, 회전을 두면 그 상황이 "이미 폐기된 토큰" 거절로 드러난다.
 */
@Entity
@Table(name = "admin_refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public AdminRefreshToken(UUID adminId, String tokenHash, OffsetDateTime expiresAt) {
        this.adminId = adminId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        return revokedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now();
        }
    }
}
