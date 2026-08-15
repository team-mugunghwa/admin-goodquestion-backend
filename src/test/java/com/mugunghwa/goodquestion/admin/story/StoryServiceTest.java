package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CreateSceneRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CreateStoryRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.ReorderScenesRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.SceneResponse;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.StoryDetail;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.UpdateStoryRequest;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class StoryServiceTest {

    @Autowired StoryService storyService;
    @Autowired SceneService sceneService;
    @Autowired TestFixture fixture;

    private AdminPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = fixture.createAdmin();
    }

    private StoryDetail createStory() {
        return storyService.create(admin, new CreateStoryRequest(
                "테스트 이야기", "줄거리", "주인공", "도입", null, "EASY", (short) 15,
                null, null, List.of("다름", "용기")));
    }

    private SceneResponse createNarrationScene(UUID storyId, String description) {
        return sceneService.create(admin, storyId, new CreateSceneRequest(
                null, SceneType.STORY, description, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("주제 이름을 보내면 없는 주제는 새로 만들어 붙인다")
    void createsMissingTopics() {
        StoryDetail story = createStory();
        assertThat(story.topics()).containsExactlyInAnyOrder("다름", "용기");
    }

    @Test
    @DisplayName("장면이 없는 이야기는 공개할 수 없다")
    void cannotPublishWithoutScenes() {
        StoryDetail story = createStory();

        // 공개했는데 장면이 없으면 사용자는 시작하자마자 빈 화면을 본다.
        assertThatThrownBy(() -> storyService.update(admin, story.id(),
                new UpdateStoryRequest(null, null, null, null, null, null, null, null,
                        StoryStatus.PUBLISHED, null)))
                .isInstanceOf(BusinessException.class);

        createNarrationScene(story.id(), "옛날 옛적에");
        StoryDetail published = storyService.update(admin, story.id(),
                new UpdateStoryRequest(null, null, null, null, null, null, null, null,
                        StoryStatus.PUBLISHED, null));
        assertThat(published.status()).isEqualTo(StoryStatus.PUBLISHED);
    }

    @Test
    @DisplayName("대화 장면인데 캐릭터나 턴 수가 빠지면 저장하지 않는다")
    void rejectsIncompleteDialogueScene() {
        StoryDetail story = createStory();

        assertThatThrownBy(() -> sceneService.create(admin, story.id(), new CreateSceneRequest(
                null, SceneType.DIALOGUE, "대화 장면", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INCOMPLETE_DIALOGUE_SCENE);
    }

    @Test
    @DisplayName("장면 순서를 바꿔도 (이야기, 순서) 유일 제약에 걸리지 않는다")
    void reorderScenes() {
        StoryDetail story = createStory();
        SceneResponse first = createNarrationScene(story.id(), "1번");
        SceneResponse second = createNarrationScene(story.id(), "2번");
        SceneResponse third = createNarrationScene(story.id(), "3번");

        List<SceneResponse> reordered = sceneService.reorder(admin, story.id(),
                new ReorderScenesRequest(List.of(third.id(), first.id(), second.id())));

        assertThat(reordered).extracting(SceneResponse::sceneDescription)
                .containsExactly("3번", "1번", "2번");
        assertThat(reordered).extracting(SceneResponse::sceneOrder)
                .containsExactly((short) 0, (short) 1, (short) 2);
    }

    @Test
    @DisplayName("진행 기록이 있는 이야기는 삭제하지 않고 보관하게 한다")
    void cannotDeleteStoryInUse() {
        StoryDetail story = createStory();
        UUID parentId = fixture.createParent("김보호자");
        UUID childId = fixture.createChild(parentId, "아이");
        fixture.createStorySession(childId, story.id());

        assertThatThrownBy(() -> storyService.delete(admin, story.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORY_IN_USE);
    }

    @Test
    @DisplayName("주제를 빈 배열로 보내면 전부 지우고, null이면 건드리지 않는다")
    void topicsNullMeansUnchanged() {
        StoryDetail story = createStory();

        StoryDetail untouched = storyService.update(admin, story.id(),
                new UpdateStoryRequest("제목만 변경", null, null, null, null, null, null, null, null, null));
        assertThat(untouched.topics()).containsExactlyInAnyOrder("다름", "용기");

        StoryDetail cleared = storyService.update(admin, story.id(),
                new UpdateStoryRequest(null, null, null, null, null, null, null, null, null, List.of()));
        assertThat(cleared.topics()).isEmpty();
    }
}
