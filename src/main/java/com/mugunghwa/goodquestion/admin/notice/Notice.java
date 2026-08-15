package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 공지사항. 관리자가 쓰고 사용자 앱이 읽는다.
 *
 * <p>같은 테이블을 서비스 백엔드도 읽는다. 컬럼을 바꾸면 그쪽 엔티티도 함께 고쳐야 한다.
 */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeCategory category;

    /** 목록 맨 위 고정. 점검 공지처럼 기간이 지나면 내려야 하는 것에 쓴다. */
    @Column(nullable = false)
    private boolean pinned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    /**
     * 공개 시점. 작성과 공개가 갈리므로 createdAt으로 대신할 수 없다.
     * DB에 "PUBLISHED면 이 값이 반드시 있다"는 제약이 걸려 있다.
     */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** 작성 시점의 관리자 이름. 계정이 지워져도 누가 썼는지는 남아야 한다. */
    @Column(name = "author_name", length = 50)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Notice(String title, String content, NoticeCategory category, boolean pinned,
                  ContentStatus status, String authorName) {
        this.title = title;
        this.content = content;
        this.category = category != null ? category : NoticeCategory.GENERAL;
        this.pinned = pinned;
        this.authorName = authorName;
        this.viewCount = 0;
        applyStatus(status != null ? status : ContentStatus.DRAFT);
    }

    public void update(String title, String content, NoticeCategory category,
                       Boolean pinned, ContentStatus status) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (category != null) this.category = category;
        if (pinned != null) this.pinned = pinned;
        if (status != null) applyStatus(status);
    }

    /**
     * 공개로 바뀌는 순간 공개 시각을 채운다.
     *
     * <p>이미 공개된 글을 다시 저장할 때 시각을 새로 찍지 않는다. 오타 하나 고쳤다고
     * 목록 맨 위로 올라오면 사용자에게는 새 공지가 온 것처럼 보인다.
     */
    private void applyStatus(ContentStatus next) {
        this.status = next;
        if (next == ContentStatus.PUBLISHED && publishedAt == null) {
            this.publishedAt = OffsetDateTime.now();
        }
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
