package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CharacterRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CharacterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private static final String TARGET_TYPE = "CHARACTER";

    private final CharacterRepository characterRepository;
    private final SceneRepository sceneRepository;
    private final StoryService storyService;
    private final AuditLogger auditLogger;

    public List<CharacterResponse> list(UUID storyId) {
        return characterRepository.findAllByStoryIdOrderByCharacterKeyAsc(storyId).stream()
                .map(CharacterResponse::from).toList();
    }

    @Transactional
    public CharacterResponse create(AdminPrincipal admin, UUID storyId, CharacterRequest request) {
        Story story = storyService.load(storyId);
        if (characterRepository.existsByStoryIdAndCharacterKey(storyId, request.characterKey())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "같은 키의 캐릭터가 이미 있습니다.");
        }

        StoryCharacter character = characterRepository.save(StoryCharacter.builder()
                .story(story)
                .characterKey(request.characterKey())
                .name(request.name())
                .personality(request.personality())
                .guidanceStyle(request.guidanceStyle())
                .ttsVoice(request.ttsVoice())
                .ttsStyle(request.ttsStyle())
                .ttsGender(request.ttsGender())
                .expressionKeys(request.expressionKeys())
                .build());

        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, character.getId(),
                "캐릭터 추가: %s / %s".formatted(story.getTitle(), character.getName()));
        return CharacterResponse.from(character);
    }

    @Transactional
    public CharacterResponse update(AdminPrincipal admin, UUID storyId, UUID characterId,
                                    CharacterRequest request) {
        StoryCharacter character = load(storyId, characterId);
        character.update(request.characterKey(), request.name(), request.personality(),
                request.guidanceStyle(), request.ttsVoice(), request.ttsStyle(),
                request.ttsGender(), request.expressionKeys());

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, characterId,
                "캐릭터 수정: %s".formatted(character.getName()));
        return CharacterResponse.from(character);
    }

    /**
     * 캐릭터를 지운다.
     *
     * <p>장면이 쓰고 있으면 거절한다. DB는 {@code on delete set null}이라 그냥 지워지고,
     * 그러면 그 장면의 목소리와 표정을 찾을 수 없게 되는데 그 사실이 사용자가 그 장면에
     * 도달할 때까지 드러나지 않는다.
     */
    @Transactional
    public void delete(AdminPrincipal admin, UUID storyId, UUID characterId) {
        StoryCharacter character = load(storyId, characterId);
        if (sceneRepository.existsByCharacterId(characterId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "이 캐릭터를 쓰는 장면이 있습니다. 장면에서 먼저 캐릭터를 바꿔 주세요.");
        }
        String name = character.getName();
        characterRepository.delete(character);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, characterId,
                "캐릭터 삭제: %s".formatted(name));
    }

    private StoryCharacter load(UUID storyId, UUID characterId) {
        StoryCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "캐릭터를 찾을 수 없습니다."));
        if (!character.getStory().getId().equals(storyId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "캐릭터를 찾을 수 없습니다.");
        }
        return character;
    }
}
