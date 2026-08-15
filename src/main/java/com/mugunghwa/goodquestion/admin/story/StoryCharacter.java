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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 이야기에 등장하는 캐릭터.
 *
 * <p>클래스명이 Character가 아닌 것은 {@code java.lang.Character}와 겹치기 때문이다.
 *
 * <p>{@code characterKey}는 표정 이미지 파일명({key}_{expression}.png)의 앞부분이다.
 * 관리자 화면에서 바꿀 수는 있지만 바꾸면 이미 올라간 이미지가 안 붙는다 -
 * 화면에서 경고를 띄우고, 여기서는 유일성만 강제한다.
 */
@Entity
@Table(name = "characters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "character_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "character_key", nullable = false, length = 64)
    private String characterKey;

    @Column(nullable = false, length = 50)
    private String name;

    /** 성격과 말투. 캐릭터 대사를 만드는 LLM의 페르소나로 그대로 들어간다. */
    @Column(nullable = false, columnDefinition = "text")
    private String personality;

    /** 아이가 막혔을 때 유도를 어떻게 드러낼지. */
    @Column(name = "guidance_style", columnDefinition = "text")
    private String guidanceStyle;

    @Column(name = "tts_voice", length = 64)
    private String ttsVoice;

    /**
     * 합성 지시문. 보이스 이름만으로는 성별이 정해지지 않으므로 성별과 연령을
     * 반드시 적는다 - 같은 보이스가 지시문에 따라 남성으로도 여성으로도 나온다.
     */
    @Column(name = "tts_style", columnDefinition = "text")
    private String ttsStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "tts_gender", length = 10)
    private TtsGender ttsGender;

    /** 이 캐릭터가 실제로 가진 표정. 없는 표정을 요구하면 서비스가 기본 표정으로 대체한다. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "expression_keys", nullable = false, columnDefinition = "text[]")
    private List<String> expressionKeys = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public StoryCharacter(Story story, String characterKey, String name, String personality,
                          String guidanceStyle, String ttsVoice, String ttsStyle,
                          TtsGender ttsGender, List<String> expressionKeys) {
        this.story = story;
        this.characterKey = characterKey;
        this.name = name;
        this.personality = personality;
        this.guidanceStyle = guidanceStyle;
        this.ttsVoice = ttsVoice;
        this.ttsStyle = ttsStyle;
        this.ttsGender = ttsGender;
        this.expressionKeys = expressionKeys != null ? expressionKeys : new ArrayList<>();
    }

    public void update(String characterKey, String name, String personality, String guidanceStyle,
                       String ttsVoice, String ttsStyle, TtsGender ttsGender,
                       List<String> expressionKeys) {
        if (characterKey != null) this.characterKey = characterKey;
        if (name != null) this.name = name;
        if (personality != null) this.personality = personality;
        if (guidanceStyle != null) this.guidanceStyle = guidanceStyle;
        if (ttsVoice != null) this.ttsVoice = ttsVoice;
        if (ttsStyle != null) this.ttsStyle = ttsStyle;
        if (ttsGender != null) this.ttsGender = ttsGender;
        if (expressionKeys != null) this.expressionKeys = expressionKeys;
    }
}
