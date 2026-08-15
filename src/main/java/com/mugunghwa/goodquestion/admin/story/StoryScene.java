package com.mugunghwa.goodquestion.admin.story;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 장면.
 *
 * <p>{@code STORY}는 내레이션만 있는 장면이고 {@code DIALOGUE}는 아이가 말하는 장면이다.
 * 대화 장면은 캐릭터, 첫 대사, 장면 목표, 턴 수가 모두 있어야 서비스가 실행할 수 있고
 * DB에도 그 검사가 걸려 있다. 관리자 화면에서 절반만 채운 채 저장되면 사용자 쪽에서
 * 장면이 시작되지 않으므로, 저장 전에 서비스 계층이 먼저 막는다.
 */
@Entity
@Table(name = "story_scenes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "scene_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryScene {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "scene_order", nullable = false)
    private short sceneOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene_type", nullable = false, length = 20)
    private SceneType sceneType;

    /** STORY면 내레이션 본문, DIALOGUE면 장면 상황 설명(분석 LLM 입력). */
    @Column(name = "scene_description", nullable = false, columnDefinition = "text")
    private String sceneDescription;

    @Column(columnDefinition = "text")
    private String conflict;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private StoryCharacter character;

    /** 화면 표시용 이름. 캐릭터를 지정하면 그 이름으로 맞춰 둔다. */
    @Column(name = "character_name", length = 50)
    private String characterName;

    /** 같은 캐릭터라도 장면마다 입장이 다르다. */
    @Column(name = "scene_stance", columnDefinition = "text")
    private String sceneStance;

    /** 음성 인식 힌트. 아이 발화는 고유명사 오인식이 가장 많다. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "proper_nouns", nullable = false, columnDefinition = "text[]")
    private List<String> properNouns = new ArrayList<>();

    @Column(name = "character_opening", columnDefinition = "text")
    private String characterOpening;

    /** 고정 마지막 대사. 최대 턴에 닿아도 이 대사로 장면을 닫는다. */
    @Column(name = "character_closing", columnDefinition = "text")
    private String characterClosing;

    @Column(name = "scene_goal", columnDefinition = "text")
    private String sceneGoal;

    /** DECISION, REASON, PERSPECTIVE 같은 생각 요소 이름. 이 장면에서 확인할 것들. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "required_elements", columnDefinition = "text[]")
    private List<String> requiredElements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "element_criteria", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> elementCriteria;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "remaining_worries", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> remainingWorries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mission_config", columnDefinition = "jsonb")
    private Map<String, Object> missionConfig;

    /** 이만큼 말하면 목표를 채웠다고 본다. */
    @Column(name = "preferred_turns")
    private Short preferredTurns;

    /** 여기 닿으면 목표와 무관하게 장면을 닫는다. */
    @Column(name = "max_turns")
    private Short maxTurns;

    @Builder
    public StoryScene(Story story, short sceneOrder, SceneType sceneType, String sceneDescription,
                      String conflict, String imageUrl, StoryCharacter character,
                      String characterName, String sceneStance, List<String> properNouns,
                      String characterOpening, String characterClosing, String sceneGoal,
                      List<String> requiredElements, Map<String, String> elementCriteria,
                      Map<String, String> remainingWorries, Map<String, Object> missionConfig,
                      Short preferredTurns, Short maxTurns) {
        this.story = story;
        this.sceneOrder = sceneOrder;
        this.sceneType = sceneType != null ? sceneType : SceneType.STORY;
        this.sceneDescription = sceneDescription;
        this.conflict = conflict;
        this.imageUrl = imageUrl;
        this.character = character;
        this.characterName = characterName;
        this.sceneStance = sceneStance;
        this.properNouns = properNouns != null ? properNouns : new ArrayList<>();
        this.characterOpening = characterOpening;
        this.characterClosing = characterClosing;
        this.sceneGoal = sceneGoal;
        this.requiredElements = requiredElements;
        this.elementCriteria = elementCriteria != null ? elementCriteria : Map.of();
        this.remainingWorries = remainingWorries != null ? remainingWorries : Map.of();
        this.missionConfig = missionConfig;
        this.preferredTurns = preferredTurns;
        this.maxTurns = maxTurns;
    }

    @SuppressWarnings("java:S107") // 장면이 실제로 가진 항목 수가 이만큼이다. 묶을 자연스러운 단위가 없다.
    public void update(SceneType sceneType, String sceneDescription, String conflict,
                       String imageUrl, StoryCharacter character, String characterName,
                       String sceneStance, List<String> properNouns, String characterOpening,
                       String characterClosing, String sceneGoal, List<String> requiredElements,
                       Map<String, String> elementCriteria, Map<String, String> remainingWorries,
                       Map<String, Object> missionConfig, Short preferredTurns, Short maxTurns) {
        if (sceneType != null) this.sceneType = sceneType;
        if (sceneDescription != null) this.sceneDescription = sceneDescription;
        if (conflict != null) this.conflict = conflict;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (character != null) this.character = character;
        if (characterName != null) this.characterName = characterName;
        if (sceneStance != null) this.sceneStance = sceneStance;
        if (properNouns != null) this.properNouns = properNouns;
        if (characterOpening != null) this.characterOpening = characterOpening;
        if (characterClosing != null) this.characterClosing = characterClosing;
        if (sceneGoal != null) this.sceneGoal = sceneGoal;
        if (requiredElements != null) this.requiredElements = requiredElements;
        if (elementCriteria != null) this.elementCriteria = elementCriteria;
        if (remainingWorries != null) this.remainingWorries = remainingWorries;
        if (missionConfig != null) this.missionConfig = missionConfig;
        if (preferredTurns != null) this.preferredTurns = preferredTurns;
        if (maxTurns != null) this.maxTurns = maxTurns;
    }

    void changeOrder(short sceneOrder) {
        this.sceneOrder = sceneOrder;
    }

    /**
     * 대화 장면으로서 서비스가 실행할 수 있는 상태인가.
     *
     * <p>DB에도 같은 검사가 있지만 여기서 먼저 본다. 제약 위반으로 올라오는 메시지는
     * 컬럼 이름의 나열이라 관리자가 무엇을 채워야 하는지 알 수 없다.
     */
    boolean isRunnable() {
        if (sceneType != SceneType.DIALOGUE) {
            return true;
        }
        return characterName != null && characterOpening != null && sceneGoal != null
                && requiredElements != null && preferredTurns != null && maxTurns != null
                && preferredTurns <= maxTurns;
    }
}
