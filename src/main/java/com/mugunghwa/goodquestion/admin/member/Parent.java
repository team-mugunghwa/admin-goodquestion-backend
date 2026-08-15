package com.mugunghwa.goodquestion.admin.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 보호자 계정. 서비스 백엔드가 만들고 관리자 콘솔은 조회와 정지만 한다.
 *
 * <p>서비스 쪽 엔티티의 컬럼을 다 매핑하지 않았다. 비밀번호 해시와 소셜 식별자는
 * 관리자 화면이 쓸 일이 없고, 매핑해 두면 실수로 응답에 실릴 길이 생긴다.
 * {@code ddl-auto=validate}는 매핑하지 않은 컬럼을 문제 삼지 않는다.
 */
@Entity
@Table(name = "parents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    /** LOCAL / KAKAO / GOOGLE */
    @Column(nullable = false, length = 20)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParentStatus status;

    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "suspended_reason", columnDefinition = "text")
    private String suspendedReason;

    /** 로그인 실패로 잠긴 계정. 관리자가 풀어 줄 수 있어야 한다. */
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    void suspend(String reason) {
        this.status = ParentStatus.SUSPENDED;
        this.suspendedAt = OffsetDateTime.now();
        this.suspendedReason = reason;
    }

    void restore() {
        this.status = ParentStatus.ACTIVE;
        this.suspendedAt = null;
        this.suspendedReason = null;
        // 정지를 푸는 김에 로그인 실패 잠금도 함께 푼다. 둘이 겹쳐 있으면
        // 관리자는 풀었다고 생각하는데 사용자는 여전히 못 들어온다.
        this.lockedUntil = null;
    }
}
