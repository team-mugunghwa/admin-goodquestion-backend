package com.mugunghwa.goodquestion.admin.auth;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.AdminRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 관리자 계정.
 *
 * <p>보호자(parents)와 테이블을 나눈 이유는 권한 컬럼 하나로 합치면 보호자 로그인
 * 경로의 어떤 실수든 곧바로 관리자 권한 문제가 되기 때문이다. 로그인 경로 자체를
 * 분리해 두면 그런 연결이 생기지 않는다.
 */
@Entity
@Table(name = "admin_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private short failedLoginAttempts;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public AdminAccount(String email, String passwordHash, String name, AdminRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role != null ? role : AdminRole.ADMIN;
        this.status = AdminStatus.ACTIVE;
        this.failedLoginAttempts = 0;
    }

    public AdminPrincipal toPrincipal() {
        return new AdminPrincipal(id, email, name, role);
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    public boolean isSuspended() {
        return status == AdminStatus.SUSPENDED;
    }

    /**
     * 로그인 실패를 기록하고 상한을 넘으면 잠근다.
     *
     * <p>서비스 쪽 보호자 계정과 달리 잠금 시간을 두 배씩 늘리지 않는다. 관리자 수는
     * 적고 서로 얼굴을 아는 사이라, 잠금이 몇 시간씩 길어지면 공격을 막기보다 운영이
     * 멈추는 쪽 손해가 크다. 고정 시간으로 두고 필요하면 다른 관리자가 풀어 준다.
     */
    void recordLoginFailure(int maxAttempts, Duration lockDuration) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = OffsetDateTime.now().plus(lockDuration);
        }
    }

    void recordLoginSuccess(String ip) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = OffsetDateTime.now();
        lastLoginIp = ip;
    }

    void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        // 비밀번호를 바꿨다는 것은 본인이 들어와 있다는 뜻이므로 잠금을 함께 푼다.
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    void updateProfile(String name, AdminRole role) {
        if (name != null) this.name = name;
        if (role != null) this.role = role;
    }

    void changeStatus(AdminStatus status) {
        this.status = status;
        if (status == AdminStatus.ACTIVE) {
            this.failedLoginAttempts = 0;
            this.lockedUntil = null;
        }
    }
}
