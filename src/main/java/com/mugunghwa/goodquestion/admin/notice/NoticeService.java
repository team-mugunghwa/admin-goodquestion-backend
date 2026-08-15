package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.NoticeDetail;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.NoticeSummary;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.UpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final String TARGET_TYPE = "NOTICE";

    private final NoticeRepository repository;
    private final AuditLogger auditLogger;

    public PageResponse<NoticeSummary> list(ContentStatus status, String keyword, int page, int size) {
        return PageResponse.of(
                repository.search(status, StringUtils.hasText(keyword) ? keyword.trim() : "",
                        PageRequest.of(page, Math.min(size, 100))),
                NoticeSummary::from);
    }

    public NoticeDetail get(UUID noticeId) {
        return NoticeDetail.from(load(noticeId));
    }

    @Transactional
    public NoticeDetail create(AdminPrincipal admin, CreateRequest request) {
        Notice notice = repository.save(Notice.builder()
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .pinned(request.pinned())
                .status(request.status())
                .authorName(admin.name())
                .build());

        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, notice.getId(),
                "공지 작성: %s (%s)".formatted(notice.getTitle(), notice.getStatus()));
        return NoticeDetail.from(notice);
    }

    @Transactional
    public NoticeDetail update(AdminPrincipal admin, UUID noticeId, UpdateRequest request) {
        Notice notice = load(noticeId);
        ContentStatus before = notice.getStatus();
        notice.update(request.title(), request.content(), request.category(),
                request.pinned(), request.status());

        // 노출 상태가 바뀐 것과 본문만 고친 것은 나중에 찾는 이유가 다르다.
        // "언제 내렸는가"를 찾을 때 UPDATE 수십 건 사이에서 골라내지 않도록 나눠 남긴다.
        boolean statusChanged = request.status() != null && request.status() != before;
        auditLogger.log(admin, statusChanged ? AuditAction.PUBLISH : AuditAction.UPDATE,
                TARGET_TYPE, notice.getId(),
                statusChanged
                        ? "공지 상태 변경: %s (%s -> %s)".formatted(notice.getTitle(), before, notice.getStatus())
                        : "공지 수정: %s".formatted(notice.getTitle()));
        return NoticeDetail.from(notice);
    }

    @Transactional
    public void delete(AdminPrincipal admin, UUID noticeId) {
        Notice notice = load(noticeId);
        String title = notice.getTitle();
        repository.delete(notice);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, noticeId, "공지 삭제: %s".formatted(title));
    }

    private Notice load(UUID noticeId) {
        return repository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
    }
}
