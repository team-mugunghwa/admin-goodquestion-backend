package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.NoticeDetail;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.NoticeSummary;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.UpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public PageResponse<NoticeSummary> list(@RequestParam(required = false) ContentStatus status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return noticeService.list(status, keyword, page, size);
    }

    @GetMapping("/{noticeId}")
    public NoticeDetail get(@PathVariable UUID noticeId) {
        return noticeService.get(noticeId);
    }

    @PostMapping
    public NoticeDetail create(@CurrentAdmin AdminPrincipal admin,
                               @Valid @RequestBody CreateRequest request) {
        return noticeService.create(admin, request);
    }

    @PatchMapping("/{noticeId}")
    public NoticeDetail update(@CurrentAdmin AdminPrincipal admin,
                               @PathVariable UUID noticeId,
                               @Valid @RequestBody UpdateRequest request) {
        return noticeService.update(admin, noticeId, request);
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID noticeId) {
        noticeService.delete(admin, noticeId);
        return ResponseEntity.noContent().build();
    }

    /** 이전 내용들. 최신이 위다. */
    @GetMapping("/{noticeId}/revisions")
    public List<RevisionResponse> revisions(@PathVariable UUID noticeId) {
        return noticeService.revisions(noticeId).stream()
                .map(RevisionResponse::from).toList();
    }

    /** 이전 내용으로 되돌리기. 공개 여부는 건드리지 않는다. */
    @PostMapping("/{noticeId}/revisions/{revisionId}/revert")
    public NoticeDetail revert(@CurrentAdmin AdminPrincipal admin,
                               @PathVariable UUID noticeId,
                               @PathVariable UUID revisionId) {
        return noticeService.revert(admin, noticeId, revisionId);
    }

    /** 예약 공개 설정. 초안에만 걸 수 있고 다시 걸면 시각이 바뀐다. */
    @PutMapping("/{noticeId}/schedule")
    public NoticeDetail schedule(@CurrentAdmin AdminPrincipal admin,
                                 @PathVariable UUID noticeId,
                                 @Valid @RequestBody ScheduleRequest request) {
        return noticeService.schedule(admin, noticeId, request.publishAt());
    }

    @DeleteMapping("/{noticeId}/schedule")
    public NoticeDetail cancelSchedule(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID noticeId) {
        return noticeService.cancelSchedule(admin, noticeId);
    }

    public record ScheduleRequest(@NotNull OffsetDateTime publishAt) {
    }

    public record RevisionResponse(
            UUID id,
            String title,
            String content,
            NoticeCategory category,
            boolean pinned,
            String editedByEmail,
            OffsetDateTime createdAt
    ) {
        static RevisionResponse from(NoticeRevision revision) {
            return new RevisionResponse(revision.getId(), revision.getTitle(),
                    revision.getContent(), revision.getCategory(), revision.isPinned(),
                    revision.getEditedByEmail(), revision.getCreatedAt());
        }
    }
}
