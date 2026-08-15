package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.CreateStoryRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.StoryDetail;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.StorySummary;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.UpdateStoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private static final String TARGET_TYPE = "STORY";

    private final StoryRepository storyRepository;
    private final TopicRepository topicRepository;
    private final StoryTopicRepository storyTopicRepository;
    private final SceneRepository sceneRepository;
    private final AuditLogger auditLogger;

    public PageResponse<StorySummary> list(StoryStatus status, String keyword, int page, int size) {
        Page<Story> stories = storyRepository.search(status,
                StringUtils.hasText(keyword) ? keyword.trim() : "",
                PageRequest.of(page, Math.min(size, 100)));

        List<UUID> ids = stories.getContent().stream().map(Story::getId).toList();
        Map<UUID, List<String>> topics = findTopicNames(ids);

        return PageResponse.of(stories, story -> new StorySummary(
                story.getId(), story.getTitle(), story.getSummary(), story.getDifficulty(),
                story.getEstimatedMinutes(), story.getImageUrl(), story.getStatus(),
                topics.getOrDefault(story.getId(), List.of()),
                sceneRepository.countByStoryId(story.getId()),
                story.getCreatedAt(), story.getUpdatedAt()));
    }

    public StoryDetail get(UUID storyId) {
        Story story = load(storyId);
        return toDetail(story);
    }

    @Transactional
    public StoryDetail create(AdminPrincipal admin, CreateStoryRequest request) {
        Story story = storyRepository.save(Story.builder()
                .title(request.title())
                .summary(request.summary())
                .childRole(request.childRole())
                .intro(request.intro())
                .imageUrl(request.imageUrl())
                .difficulty(request.difficulty())
                .estimatedMinutes(request.estimatedMinutes())
                .postActivityConfig(request.postActivityConfig())
                .status(request.status())
                .build());

        if (request.topics() != null) {
            replaceTopics(story, request.topics());
        }

        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, story.getId(),
                "이야기 생성: %s".formatted(story.getTitle()));
        return toDetail(story);
    }

    @Transactional
    public StoryDetail update(AdminPrincipal admin, UUID storyId, UpdateStoryRequest request) {
        Story story = load(storyId);
        StoryStatus before = story.getStatus();

        if (request.status() == StoryStatus.PUBLISHED && sceneRepository.countByStoryId(storyId) == 0) {
            // 장면이 없는 이야기를 공개하면 사용자가 시작하자마자 아무것도 없는 화면을 본다.
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "장면이 없는 이야기는 공개할 수 없습니다. 장면을 먼저 추가해 주세요.");
        }

        story.update(request.title(), request.summary(), request.childRole(), request.intro(),
                request.imageUrl(), request.difficulty(), request.estimatedMinutes(),
                request.postActivityConfig(), request.status());

        // null과 빈 배열이 다르다. null은 "주제는 건드리지 마라", 빈 배열은 "전부 지워라".
        if (request.topics() != null) {
            replaceTopics(story, request.topics());
        }

        boolean statusChanged = request.status() != null && request.status() != before;
        auditLogger.log(admin, statusChanged ? AuditAction.PUBLISH : AuditAction.UPDATE,
                TARGET_TYPE, storyId,
                statusChanged
                        ? "이야기 상태 변경: %s (%s -> %s)".formatted(story.getTitle(), before, story.getStatus())
                        : "이야기 수정: %s".formatted(story.getTitle()));
        return toDetail(story);
    }

    /**
     * 이야기를 지운다.
     *
     * <p>진행 기록이 있으면 거절한다. story_sessions가 이야기를 참조하고 있어 DB가
     * 막기도 하지만, 그보다 아이의 학습 기록과 보호자 리포트가 그 이야기를 가리키고
     * 있다는 것이 이유다. 지우면 리포트에서 "무엇을 하고 받은 평가인지"가 사라진다.
     * 노출만 멈추려는 것이라면 보관(ARCHIVED)이 맞다.
     */
    @Transactional
    public void delete(AdminPrincipal admin, UUID storyId) {
        Story story = load(storyId);
        if (storyRepository.countSessions(storyId) > 0) {
            throw new BusinessException(ErrorCode.STORY_IN_USE);
        }
        String title = story.getTitle();
        // 장면과 캐릭터, 주제 연결은 DB의 on delete cascade가 함께 지운다.
        storyRepository.delete(story);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, storyId,
                "이야기 삭제: %s".formatted(title));
    }

    Story load(UUID storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다."));
    }

    /**
     * 주제를 통째로 갈아 끼운다.
     *
     * <p>없는 이름은 새로 만든다. 관리자가 주제를 먼저 등록하고 이야기로 돌아와 고르는
     * 두 단계를 거치지 않아도 되게 하려는 것이다. 오타로 비슷한 주제가 늘어날 수 있어
     * 주제 마스터 화면에서 정리할 수 있게 해 두었다.
     */
    private void replaceTopics(Story story, List<String> topicNames) {
        storyTopicRepository.deleteAllByStoryId(story.getId());
        // delete와 insert가 같은 트랜잭션에 있어, 플러시하지 않으면 삽입이 먼저 나가
        // 기본키 충돌이 난다.
        storyTopicRepository.flush();

        topicNames.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(name -> {
                    Topic topic = topicRepository.findByName(name)
                            .orElseGet(() -> topicRepository.save(Topic.builder()
                                    .name(name)
                                    .displayOrder((short) topicRepository.count())
                                    .build()));
                    storyTopicRepository.save(new StoryTopic(story, topic));
                });
    }

    private StoryDetail toDetail(Story story) {
        List<String> topics = findTopicNames(List.of(story.getId()))
                .getOrDefault(story.getId(), List.of());
        return new StoryDetail(story.getId(), story.getTitle(), story.getSummary(),
                story.getChildRole(), story.getIntro(), story.getImageUrl(), story.getDifficulty(),
                story.getEstimatedMinutes(), story.getPostActivityConfig(), story.getStatus(),
                topics, sceneRepository.countByStoryId(story.getId()),
                storyRepository.countSessions(story.getId()),
                story.getCreatedAt(), story.getUpdatedAt());
    }

    /** 이야기별 주제 이름을 한 번에 가져온다. 이야기 수만큼 쿼리가 나가지 않게. */
    private Map<UUID, List<String>> findTopicNames(List<UUID> storyIds) {
        if (storyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return storyTopicRepository.findAllByStoryIds(storyIds).stream()
                .collect(Collectors.groupingBy(
                        st -> st.getStory().getId(),
                        Collectors.mapping(st -> st.getTopic().getName(), Collectors.toList())));
    }
}
