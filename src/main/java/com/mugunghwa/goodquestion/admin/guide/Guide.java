package com.mugunghwa.goodquestion.admin.guide;

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
 * 이용안내 문서. "이야기는 어떻게 시작하나요?" 같은 도움말 한 편이 한 행이다.
 *
 * <p>공지와 달리 시간순이 아니라 관리자가 정한 순서로 노출된다. 도움말은 읽는 순서가
 * 있고(가입 -> 첫 이야기 -> 보상), 최신 글이 위로 올라오면 그 흐름이 매번 흐트러진다.
 */
@Entity
@Table(name = "guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** 카테고리 안에서의 노출 순서. 작을수록 위다. */
    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Guide(GuideCategory category, String title, String content,
                 Short displayOrder, ContentStatus status) {
        this.category = category != null ? category : GuideCategory.BASIC;
        this.title = title;
        this.content = content;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.status = status != null ? status : ContentStatus.DRAFT;
    }

    public void update(GuideCategory category, String title, String content,
                       Short displayOrder, ContentStatus status) {
        if (category != null) this.category = category;
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (status != null) this.status = status;
    }

    void changeOrder(short displayOrder) {
        this.displayOrder = displayOrder;
    }
}
