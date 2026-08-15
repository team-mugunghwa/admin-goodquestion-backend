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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
