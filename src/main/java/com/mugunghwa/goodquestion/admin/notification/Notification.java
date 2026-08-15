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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 사용자 알림함의 한 줄.
 *
 * <p>푸시는 기기가 꺼져 있거나 권한이 없거나 토큰이 만료되면 도착하지 않는다.
 * "답변이 등록되면 사용자가 확인할 수 있다"가 성립하려면 앱 안에서 다시 볼 수 있어야
 * 하므로, 푸시와 별개로 알림을 여기 쌓는다. 푸시는 이 행이 생겼다고 알리는 수단이다.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    /** 앱이 이동할 화면 경로. 예: {@code /support/{inquiryId}} */
    @Column(name = "link_path", length = 200)
    private String linkPath;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Notification(UUID parentId, NotificationType type, String title, String body, String linkPath) {
        this.parentId = parentId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkPath = linkPath;
    }
}
