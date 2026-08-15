package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CharacterRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CharacterResponse;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CreateSceneRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CreateStoryRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.ReorderScenesRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.SceneResponse;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.StoryDetail;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.StorySummary;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.UpdateSceneRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.UpdateStoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 이야기 관리.
 *
 * <p>장면과 캐릭터는 이야기에 속하므로 하위 경로에 둔다. 별도 최상위 경로로 두면
 * "어느 이야기의 장면인가"를 매번 쿼리 파라미터로 실어야 하고 소유 관계가 흐려진다.
 */
@RestController
@RequestMapping("/api/admin/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final SceneService sceneService;
    private final CharacterService characterService;

    @GetMapping
    public PageResponse<StorySummary> list(@RequestParam(required = false) StoryStatus status,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return storyService.list(status, keyword, page, size);
    }

    @GetMapping("/{storyId}")
    public StoryDetail get(@PathVariable UUID storyId) {
        return storyService.get(storyId);
    }

    @PostMapping
    public StoryDetail create(@CurrentAdmin AdminPrincipal admin,
                              @Valid @RequestBody CreateStoryRequest request) {
        return storyService.create(admin, request);
    }

    @PatchMapping("/{storyId}")
    public StoryDetail update(@CurrentAdmin AdminPrincipal admin,
                              @PathVariable UUID storyId,
                              @Valid @RequestBody UpdateStoryRequest request) {
        return storyService.update(admin, storyId, request);
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> delete(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID storyId) {
        storyService.delete(admin, storyId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ 장면

    @GetMapping("/{storyId}/scenes")
    public List<SceneResponse> listScenes(@PathVariable UUID storyId) {
        return sceneService.list(storyId);
    }

    @PostMapping("/{storyId}/scenes")
    public SceneResponse createScene(@CurrentAdmin AdminPrincipal admin,
                                     @PathVariable UUID storyId,
                                     @Valid @RequestBody CreateSceneRequest request) {
        return sceneService.create(admin, storyId, request);
    }

    @PatchMapping("/{storyId}/scenes/{sceneId}")
    public SceneResponse updateScene(@CurrentAdmin AdminPrincipal admin,
                                     @PathVariable UUID storyId,
                                     @PathVariable UUID sceneId,
                                     @Valid @RequestBody UpdateSceneRequest request) {
        return sceneService.update(admin, storyId, sceneId, request);
    }

    @PutMapping("/{storyId}/scenes/order")
    public List<SceneResponse> reorderScenes(@CurrentAdmin AdminPrincipal admin,
                                             @PathVariable UUID storyId,
                                             @Valid @RequestBody ReorderScenesRequest request) {
        return sceneService.reorder(admin, storyId, request);
    }

    @DeleteMapping("/{storyId}/scenes/{sceneId}")
    public ResponseEntity<Void> deleteScene(@CurrentAdmin AdminPrincipal admin,
                                            @PathVariable UUID storyId,
                                            @PathVariable UUID sceneId) {
        sceneService.delete(admin, storyId, sceneId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- 캐릭터

    @GetMapping("/{storyId}/characters")
    public List<CharacterResponse> listCharacters(@PathVariable UUID storyId) {
        return characterService.list(storyId);
    }

    @PostMapping("/{storyId}/characters")
    public CharacterResponse createCharacter(@CurrentAdmin AdminPrincipal admin,
                                             @PathVariable UUID storyId,
                                             @Valid @RequestBody CharacterRequest request) {
        return characterService.create(admin, storyId, request);
    }

    @PatchMapping("/{storyId}/characters/{characterId}")
    public CharacterResponse updateCharacter(@CurrentAdmin AdminPrincipal admin,
                                             @PathVariable UUID storyId,
                                             @PathVariable UUID characterId,
                                             @Valid @RequestBody CharacterRequest request) {
        return characterService.update(admin, storyId, characterId, request);
    }

    @DeleteMapping("/{storyId}/characters/{characterId}")
    public ResponseEntity<Void> deleteCharacter(@CurrentAdmin AdminPrincipal admin,
                                                @PathVariable UUID storyId,
                                                @PathVariable UUID characterId) {
        characterService.delete(admin, storyId, characterId);
        return ResponseEntity.noContent().build();
    }
}
