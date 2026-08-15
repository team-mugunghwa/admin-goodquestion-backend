package com.mugunghwa.goodquestion.admin.story;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.TopicRequest;
import com.mugunghwa.goodquestion.admin.story.dto.StoryDtos.TopicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 주제 마스터. 사용자 앱 이야기 목록의 필터 칩이 이 순서를 따른다. */
@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public List<TopicResponse> list() {
        return topicService.list();
    }

    /** 이 주제를 쓰는 이야기 수. 삭제 확인 문구에 쓴다. */
    @GetMapping("/{topicId}/usage")
    public UsageResponse usage(@PathVariable UUID topicId) {
        return new UsageResponse(topicService.countStories(topicId));
    }

    @PostMapping
    public TopicResponse create(@CurrentAdmin AdminPrincipal admin,
                                @Valid @RequestBody TopicRequest request) {
        return topicService.create(admin, request);
    }

    @PatchMapping("/{topicId}")
    public TopicResponse update(@CurrentAdmin AdminPrincipal admin,
                                @PathVariable UUID topicId,
                                @Valid @RequestBody TopicRequest request) {
        return topicService.update(admin, topicId, request);
    }

    @DeleteMapping("/{topicId}")
    public ResponseEntity<Void> delete(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID topicId) {
        topicService.delete(admin, topicId);
        return ResponseEntity.noContent().build();
    }

    public record UsageResponse(long storyCount) {
    }
}
