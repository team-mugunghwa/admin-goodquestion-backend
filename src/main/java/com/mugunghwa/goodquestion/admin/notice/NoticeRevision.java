package com.mugunghwa.goodquestion.admin.notice;

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
 * 공지의 이전 내용 한 장.
 *
 * <p>저장할 때마다 바꾸기 전의 내용을 남긴다. 상태(공개 여부)는 담지 않는다 -
 * 되돌리기는 "글 내용을 예전으로" 이지 "공개를 취소"가 아니다. 공개 여부는
 * 별도 조작으로 다루고 감사 로그가 그 이력을 안다.
 */
@Entity
@Table(name = "admin_notice_revisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 저장 순서. DB 가 매기고 정렬은 이 값을 쓴다 - created_at 은 한 트랜잭션 안에서 같다. */
    @Column(nullable = false, insertable = false, updatable = false)
    private long seq;

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeCategory category;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "edited_by_email", nullable = false, length = 255)
    private String editedByEmail;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public NoticeRevision(UUID noticeId, String title, String content,
                          NoticeCategory category, boolean pinned, String editedByEmail) {
        this.noticeId = noticeId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.pinned = pinned;
        this.editedByEmail = editedByEmail;
    }

    /** 지금 공지 내용을 그대로 떠 둔다. */
    static NoticeRevision snapshotOf(Notice notice, String editedByEmail) {
        return NoticeRevision.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                .pinned(notice.isPinned())
                .editedByEmail(editedByEmail)
                .build();
    }
}
