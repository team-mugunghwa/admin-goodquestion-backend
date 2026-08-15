package com.mugunghwa.goodquestion.admin.notification;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 푸시 발송 대상 기기(FCM 등록 토큰).
 *
 * <p>등록은 사용자 앱이 서비스 백엔드에 한다. 관리자 콘솔은 읽고, 발송이 실패했을 때
 * 비활성으로 표시하는 것까지만 한다.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    /**
     * 발송이 "등록되지 않은 토큰"으로 거절된 시각.
     *
     * <p>바로 지우지 않는다. 지우면 다음 발송 때 왜 이 사용자에게 안 갔는지 확인할
     * 근거가 사라진다. 발송 대상 조회에서는 이 값이 없는 것만 고른다.
     */
    @Column(name = "disabled_at")
    private OffsetDateTime disabledAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    public void disable() {
        if (disabledAt == null) {
            disabledAt = OffsetDateTime.now();
        }
    }
}
