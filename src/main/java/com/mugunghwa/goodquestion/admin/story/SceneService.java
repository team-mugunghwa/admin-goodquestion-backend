package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CreateSceneRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.ReorderScenesRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.SceneResponse;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.UpdateSceneRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneService {

    private static final String TARGET_TYPE = "SCENE";

    private final SceneRepository sceneRepository;
    private final CharacterRepository characterRepository;
    private final StoryService storyService;
    private final AuditLogger auditLogger;

    public List<SceneResponse> list(UUID storyId) {
        return sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(storyId).stream()
                .map(SceneResponse::from).toList();
    }

    @Transactional
    public SceneResponse create(AdminPrincipal admin, UUID storyId, CreateSceneRequest request) {
        Story story = storyService.load(storyId);
        short order = request.sceneOrder() != null
                ? request.sceneOrder()
                : (short) (sceneRepository.findMaxSceneOrder(storyId) + 1);

        if (sceneRepository.findByStoryIdAndSceneOrder(storyId, order).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_SCENE_ORDER);
        }

        StoryScene scene = StoryScene.builder()
                .story(story)
                .sceneOrder(order)
                .sceneType(request.sceneType())
                .sceneDescription(request.sceneDescription())
                .conflict(request.conflict())
                .imageUrl(request.imageUrl())
                .character(resolveCharacter(storyId, request.characterId()))
                .characterName(request.characterName())
                .sceneStance(request.sceneStance())
                .properNouns(request.properNouns())
                .characterOpening(request.characterOpening())
                .characterClosing(request.characterClosing())
                .sceneGoal(request.sceneGoal())
                .requiredElements(request.requiredElements())
                .elementCriteria(request.elementCriteria())
                .remainingWorries(request.remainingWorries())
                .missionConfig(request.missionConfig())
                .preferredTurns(request.preferredTurns())
                .maxTurns(request.maxTurns())
                .build();
        requireRunnable(scene);

        sceneRepository.save(scene);
        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, scene.getId(),
                "장면 추가: %s #%d".formatted(story.getTitle(), order));
        return SceneResponse.from(scene);
    }

    @Transactional
    public SceneResponse update(AdminPrincipal admin, UUID storyId, UUID sceneId,
                                UpdateSceneRequest request) {
        StoryScene scene = load(storyId, sceneId);
        scene.update(request.sceneType(), request.sceneDescription(), request.conflict(),
                request.imageUrl(), resolveCharacter(storyId, request.characterId()),
                request.characterName(), request.sceneStance(), request.properNouns(),
                request.characterOpening(), request.characterClosing(), request.sceneGoal(),
                request.requiredElements(), request.elementCriteria(), request.remainingWorries(),
                request.missionConfig(), request.preferredTurns(), request.maxTurns());
        requireRunnable(scene);

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, sceneId,
                "장면 수정: #%d".formatted(scene.getSceneOrder()));
        return SceneResponse.from(scene);
    }

    /**
     * 장면 순서를 배열 순서대로 다시 매긴다.
     *
     * <p>{@code (story_id, scene_order)}에 유일 제약이 걸려 있어서 한 장면씩 바꾸면
     * 중간에 반드시 충돌한다(1번을 2번으로 바꾸는 순간 기존 2번과 겹친다). 먼저 전부
     * 겹치지 않는 임시 번호로 밀어 두고, 플러시한 뒤 최종 번호를 매긴다.
     */
    @Transactional
    public List<SceneResponse> reorder(AdminPrincipal admin, UUID storyId,
                                       ReorderScenesRequest request) {
        List<StoryScene> scenes = sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(storyId);
        Map<UUID, StoryScene> byId = scenes.stream()
                .collect(Collectors.toMap(StoryScene::getId, Function.identity()));

        short temp = 1000;
        for (StoryScene scene : scenes) {
            scene.changeOrder(temp++);
        }
        sceneRepository.flush();

        short order = 0;
        Set<UUID> placed = new LinkedHashSet<>();
        for (UUID sceneId : request.sceneIds()) {
            StoryScene scene = byId.get(sceneId);
            if (scene == null || !placed.add(sceneId)) continue;
            scene.changeOrder(order++);
        }
        // 요청에 빠진 장면은 기존 순서를 유지한 채 뒤에 붙는다.
        for (StoryScene scene : scenes) {
            if (placed.contains(scene.getId())) continue;
            scene.changeOrder(order++);
        }
        sceneRepository.flush();

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, storyId,
                "장면 순서 변경 %d건".formatted(order));
        return list(storyId);
    }

    @Transactional
    public void delete(AdminPrincipal admin, UUID storyId, UUID sceneId) {
        StoryScene scene = load(storyId, sceneId);
        short order = scene.getSceneOrder();
        sceneRepository.delete(scene);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, sceneId,
                "장면 삭제: #%d".formatted(order));
    }

    private void requireRunnable(StoryScene scene) {
        if (!scene.isRunnable()) {
            throw new BusinessException(ErrorCode.INCOMPLETE_DIALOGUE_SCENE);
        }
    }

    private StoryCharacter resolveCharacter(UUID storyId, UUID characterId) {
        if (characterId == null) {
            return null;
        }
        StoryCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "캐릭터를 찾을 수 없습니다."));
        if (!character.getStory().getId().equals(storyId)) {
            // 다른 이야기의 캐릭터를 붙이면 그 장면의 목소리와 표정이 엉뚱하게 나온다.
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "다른 이야기의 캐릭터입니다.");
        }
        return character;
    }

    private StoryScene load(UUID storyId, UUID sceneId) {
        StoryScene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "장면을 찾을 수 없습니다."));
        if (!scene.getStory().getId().equals(storyId)) {
            // 경로의 이야기와 장면이 어긋나면 404로 본다. 존재 여부를 알려 줄 이유가 없다.
            throw new BusinessException(ErrorCode.NOT_FOUND, "장면을 찾을 수 없습니다.");
        }
        return scene;
    }
}
