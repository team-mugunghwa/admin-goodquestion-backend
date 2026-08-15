package com.mugunghwa.goodquestion.admin.story.dto;

import com.mugunghwa.goodquestion.admin.story.SceneType;
import com.mugunghwa.goodquestion.admin.story.StoryCharacter;
import com.mugunghwa.goodquestion.admin.story.StoryScene;
import com.mugunghwa.goodquestion.admin.story.StoryStatus;
import com.mugunghwa.goodquestion.admin.story.Topic;
import com.mugunghwa.goodquestion.admin.story.TtsGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StoryDtos {

    private StoryDtos() {
    }

    // ---------------------------------------------------------------- 이야기

    public record CreateStoryRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank String summary,
            @Size(max = 50) String childRole,
            String intro,
            String imageUrl,
            String difficulty,
            Short estimatedMinutes,
            Map<String, Object> postActivityConfig,
            StoryStatus status,
            /** 주제 이름 목록. 없는 이름은 새로 만든다. */
            List<String> topics
    ) {
    }

    public record UpdateStoryRequest(
            @Size(max = 100) String title,
            String summary,
            @Size(max = 50) String childRole,
            String intro,
            String imageUrl,
            String difficulty,
            Short estimatedMinutes,
            Map<String, Object> postActivityConfig,
            StoryStatus status,
            /** null이면 주제를 건드리지 않는다. 빈 배열이면 전부 지운다. */
            List<String> topics
    ) {
    }

    /**
     * 목록 한 줄.
     *
     * @param sceneCount 장면 수. 0인 이야기를 공개하면 사용자가 시작하자마자 멈추므로
     *                   목록에서 바로 보여야 한다.
     */
    public record StorySummary(
            UUID id,
            String title,
            String summary,
            String difficulty,
            Short estimatedMinutes,
            String imageUrl,
            StoryStatus status,
            List<String> topics,
            int sceneCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record StoryDetail(
            UUID id,
            String title,
            String summary,
            String childRole,
            String intro,
            String imageUrl,
            String difficulty,
            Short estimatedMinutes,
            Map<String, Object> postActivityConfig,
            StoryStatus status,
            List<String> topics,
            int sceneCount,
            /** 이 이야기로 시작된 세션 수. 0보다 크면 삭제할 수 없다. */
            long sessionCount,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    // ---------------------------------------------------------------- 주제

    public record TopicRequest(
            @NotBlank @Size(max = 30) String name,
            Short displayOrder
    ) {
    }

    public record TopicResponse(UUID id, String name, short displayOrder) {
        public static TopicResponse from(Topic topic) {
            return new TopicResponse(topic.getId(), topic.getName(), topic.getDisplayOrder());
        }
    }

    // ---------------------------------------------------------------- 캐릭터

    public record CharacterRequest(
            @NotBlank @Size(max = 64) String characterKey,
            @NotBlank @Size(max = 50) String name,
            @NotBlank String personality,
            String guidanceStyle,
            String ttsVoice,
            String ttsStyle,
            TtsGender ttsGender,
            List<String> expressionKeys
    ) {
    }

    public record CharacterResponse(
            UUID id,
            String characterKey,
            String name,
            String personality,
            String guidanceStyle,
            String ttsVoice,
            String ttsStyle,
            TtsGender ttsGender,
            List<String> expressionKeys
    ) {
        public static CharacterResponse from(StoryCharacter character) {
            return new CharacterResponse(character.getId(), character.getCharacterKey(),
                    character.getName(), character.getPersonality(), character.getGuidanceStyle(),
                    character.getTtsVoice(), character.getTtsStyle(), character.getTtsGender(),
                    character.getExpressionKeys());
        }
    }

    // ---------------------------------------------------------------- 장면

    public record CreateSceneRequest(
            /** 비우면 맨 뒤에 붙는다. */
            Short sceneOrder,
            SceneType sceneType,
            @NotBlank String sceneDescription,
            String conflict,
            String imageUrl,
            UUID characterId,
            @Size(max = 50) String characterName,
            String sceneStance,
            List<String> properNouns,
            String characterOpening,
            String characterClosing,
            String sceneGoal,
            List<String> requiredElements,
            Map<String, String> elementCriteria,
            Map<String, String> remainingWorries,
            Map<String, Object> missionConfig,
            Short preferredTurns,
            Short maxTurns
    ) {
    }

    public record UpdateSceneRequest(
            SceneType sceneType,
            String sceneDescription,
            String conflict,
            String imageUrl,
            UUID characterId,
            @Size(max = 50) String characterName,
            String sceneStance,
            List<String> properNouns,
            String characterOpening,
            String characterClosing,
            String sceneGoal,
            List<String> requiredElements,
            Map<String, String> elementCriteria,
            Map<String, String> remainingWorries,
            Map<String, Object> missionConfig,
            Short preferredTurns,
            Short maxTurns
    ) {
    }

    /** 장면 순서 일괄 변경. 배열의 위치가 곧 순서다. */
    public record ReorderScenesRequest(@NotEmpty List<UUID> sceneIds) {
    }

    public record SceneResponse(
            UUID id,
            short sceneOrder,
            SceneType sceneType,
            String sceneDescription,
            String conflict,
            String imageUrl,
            UUID characterId,
            String characterName,
            String sceneStance,
            List<String> properNouns,
            String characterOpening,
            String characterClosing,
            String sceneGoal,
            List<String> requiredElements,
            Map<String, String> elementCriteria,
            Map<String, String> remainingWorries,
            Map<String, Object> missionConfig,
            Short preferredTurns,
            Short maxTurns
    ) {
        public static SceneResponse from(StoryScene scene) {
            return new SceneResponse(scene.getId(), scene.getSceneOrder(), scene.getSceneType(),
                    scene.getSceneDescription(), scene.getConflict(), scene.getImageUrl(),
                    scene.getCharacter() == null ? null : scene.getCharacter().getId(),
                    scene.getCharacterName(), scene.getSceneStance(), scene.getProperNouns(),
                    scene.getCharacterOpening(), scene.getCharacterClosing(), scene.getSceneGoal(),
                    scene.getRequiredElements(), scene.getElementCriteria(),
                    scene.getRemainingWorries(), scene.getMissionConfig(),
                    scene.getPreferredTurns(), scene.getMaxTurns());
        }
    }
}
