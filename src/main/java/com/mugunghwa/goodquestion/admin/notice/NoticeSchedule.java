package com.mugunghwa.goodquestion.admin.notice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 공지 예약 공개. 시각이 되면 스케줄러가 공개로 바꾸고 이 행을 지운다.
 *
 * <p>공지 하나에 예약 하나라 공지 id가 곧 기본키다. 예약을 바꾸면 행을 갈아끼운다.
 */
@Entity
@Table(name = "admin_notice_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeSchedule {

    @Id
    @Column(name = "notice_id")
    private UUID noticeId;

    @Column(name = "publish_at", nullable = false)
    private OffsetDateTime publishAt;

    /** 누가 예약했는지. 스케줄러가 공개할 때 감사 로그에 이 이름으로 남긴다. */
    @Column(name = "created_by_email", nullable = false, length = 255)
    private String createdByEmail;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public NoticeSchedule(UUID noticeId, OffsetDateTime publishAt, String createdByEmail) {
        this.noticeId = noticeId;
        this.publishAt = publishAt;
        this.createdByEmail = createdByEmail;
    }
}
