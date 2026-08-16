package com.mugunghwa.goodquestion.admin.story;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 이야기. 서비스 백엔드와 같은 테이블을 본다.
 *
 * <p>서비스 쪽 엔티티는 읽기 전용이지만 여기서는 고칠 수 있어야 하므로 수정 메서드가 있다.
 * {@code updated_at}은 관리자 마이그레이션이 추가한 컬럼이고 서비스 쪽은 매핑하지 않는다.
 */
@Entity
@Table(name = "stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    /** 아이가 맡는 역할. 상세 화면에 표시된다. */
    @Column(name = "child_role", length = 50)
    private String childRole;

    /** 도입/상황 소개. 상세 화면에 표시된다. */
    @Column(columnDefinition = "text")
    private String intro;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "estimated_minutes")
    private Short estimatedMinutes;

    /**
     * 후속 활동 설정(카드 내용과 정답 순서, 재구성 키워드 등).
     *
     * <p>구조를 컬럼으로 펴지 않고 jsonb로 둔 것은 서비스 쪽 결정이다. 관리자 화면도
     * 그 구조를 해석하지 않고 그대로 주고받는다 - 여기서 모양을 강제하면 서비스가
     * 후속 활동을 바꿀 때마다 관리자 배포가 함께 필요해진다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "post_activity_config", columnDefinition = "jsonb")
    private Map<String, Object> postActivityConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public Story(String title, String summary, String childRole, String intro, String imageUrl,
                 String difficulty, Short estimatedMinutes,
                 Map<String, Object> postActivityConfig, StoryStatus status) {
        this.title = title;
        this.summary = summary;
        this.childRole = childRole;
        this.intro = intro;
        this.imageUrl = imageUrl;
        this.difficulty = difficulty != null ? difficulty : "EASY";
        this.estimatedMinutes = estimatedMinutes;
        this.postActivityConfig = postActivityConfig;
        this.status = status != null ? status : StoryStatus.DRAFT;
    }

    public void update(String title, String summary, String childRole, String intro,
                       String imageUrl, String difficulty, Short estimatedMinutes,
                       Map<String, Object> postActivityConfig, StoryStatus status) {
        if (title != null) this.title = title;
        if (summary != null) this.summary = summary;
        if (childRole != null) this.childRole = childRole;
        if (intro != null) this.intro = intro;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (difficulty != null) this.difficulty = difficulty;
        if (estimatedMinutes != null) this.estimatedMinutes = estimatedMinutes;
        if (postActivityConfig != null) this.postActivityConfig = postActivityConfig;
        if (status != null) this.status = status;
    }
}
