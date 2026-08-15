package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.TopicRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.TopicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 주제 마스터 관리.
 *
 * <p>이야기 저장에서 없는 주제를 자동으로 만들기 때문에 오타로 비슷한 주제가 늘어날 수
 * 있다. 이 화면이 그것을 고치는 자리다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicService {

    private static final String TARGET_TYPE = "TOPIC";

    private final TopicRepository topicRepository;
    private final StoryTopicRepository storyTopicRepository;
    private final AuditLogger auditLogger;

    public List<TopicResponse> list() {
        return topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(TopicResponse::from).toList();
    }

    @Transactional
    public TopicResponse create(AdminPrincipal admin, TopicRequest request) {
        if (topicRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_TOPIC);
        }
        Topic topic = topicRepository.save(Topic.builder()
                .name(request.name())
                .displayOrder(request.displayOrder() != null
                        ? request.displayOrder() : (short) topicRepository.count())
                .build());

        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, topic.getId(),
                "주제 추가: %s".formatted(topic.getName()));
        return TopicResponse.from(topic);
    }

    @Transactional
    public TopicResponse update(AdminPrincipal admin, UUID topicId, TopicRequest request) {
        Topic topic = load(topicId);
        // 이름을 바꾸면 그 주제를 쓰던 이야기들의 표시도 함께 바뀐다(연결은 id로 걸려 있다).
        topic.update(request.name(), request.displayOrder());

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, topicId,
                "주제 수정: %s".formatted(topic.getName()));
        return TopicResponse.from(topic);
    }

    /**
     * 주제를 지운다. 이야기에 붙어 있던 연결은 DB의 cascade가 함께 지운다.
     *
     * <p>막지 않는 이유: 이야기에서 주제 하나가 빠지는 것은 되돌리기 쉽고, 잘못 만든
     * 주제를 지우지 못하면 목록 필터에 계속 남는다.
     */
    @Transactional
    public void delete(AdminPrincipal admin, UUID topicId) {
        Topic topic = load(topicId);
        String name = topic.getName();
        topicRepository.delete(topic);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, topicId, "주제 삭제: %s".formatted(name));
    }

    /** 이 주제를 쓰는 이야기 수. 삭제 전에 화면이 확인 문구를 띄우는 데 쓴다. */
    public long countStories(UUID topicId) {
        return storyTopicRepository.countByTopicId(topicId);
    }

    private Topic load(UUID topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주제를 찾을 수 없습니다."));
    }
}
