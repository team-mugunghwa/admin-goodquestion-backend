package com.mugunghwa.goodquestion.admin.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 학습 세션(이야기 한 편의 진행 기록). 관리자는 조회만 한다.
 *
 * <p>서비스 쪽 엔티티에는 진행 판단에 쓰는 필드가 스무 개 넘게 더 있다. 관리자 화면이
 * 보여주는 것은 "누가 무엇을 언제 어디까지 했는가"뿐이라 그것만 매핑했다.
 *
 * <p>{@code safetyFlagged}는 예외다. 아이 발화에서 위험 신호가 감지된 세션인데,
 * 이것을 관리자가 볼 수 없으면 감지해 둔 의미가 없다. 다만 발화 원문은 매핑하지
 * 않는다 - 확인해야 할 것은 "그런 세션이 있었다"이지 아이가 한 말 자체가 아니다.
 */
@Entity
@Table(name = "story_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorySession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    /** IN_PROGRESS / POST_ACTIVITY / COMPLETED / STOPPED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "safety_flagged", nullable = false)
    private boolean safetyFlagged;

    @Column(name = "started_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "last_activity_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime lastActivityAt;
}
