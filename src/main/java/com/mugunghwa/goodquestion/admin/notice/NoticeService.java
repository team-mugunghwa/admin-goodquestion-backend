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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final String TARGET_TYPE = "NOTICE";

    /**
     * 공지 하나가 들고 있는 이력의 상한. 넘으면 오래된 것부터 지운다.
     * 자주 고치는 공지의 이력이 무한히 쌓이는 것을 막는다.
     */
    static final int REVISION_KEEP = 20;

    private final NoticeRepository repository;
    private final NoticeRevisionRepository revisionRepository;
    private final NoticeScheduleRepository scheduleRepository;
    private final AuditLogger auditLogger;

    public PageResponse<NoticeSummary> list(ContentStatus status, String keyword, int page, int size) {
        return PageResponse.of(
                repository.search(status, StringUtils.hasText(keyword) ? keyword.trim() : "",
                        PageRequest.of(page, Math.min(size, 100))),
                NoticeSummary::from);
    }

    public NoticeDetail get(UUID noticeId) {
        return detailOf(load(noticeId));
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
        return detailOf(notice);
    }

    @Transactional
    public NoticeDetail update(AdminPrincipal admin, UUID noticeId, UpdateRequest request) {
        Notice notice = load(noticeId);
        ContentStatus before = notice.getStatus();

        // 내용이 실제로 바뀔 때만 바꾸기 전 상태를 이력으로 남긴다. 공개/비공개
        // 전환처럼 상태만 만지는 저장까지 남기면 같은 내용의 이력이 쌓인다.
        if (contentChanges(notice, request)) {
            saveRevision(notice, admin.email());
        }

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
        return detailOf(notice);
    }

    /** 이 공지의 이전 내용들. 최신이 위다. */
    public List<NoticeRevision> revisions(UUID noticeId) {
        load(noticeId);
        return revisionRepository.findAllByNoticeIdOrderBySeqDesc(noticeId);
    }

    /**
     * 이전 내용으로 되돌린다.
     *
     * <p>되돌리기 전의 지금 내용도 이력으로 남긴다. 되돌렸다가 "아까가 낫네"가
     * 되면 다시 돌아올 길이 있어야 한다. 공개 여부는 건드리지 않는다 - 되돌리기는
     * "글 내용을 예전으로"이지 "공개 취소"가 아니다.
     */
    @Transactional
    public NoticeDetail revert(AdminPrincipal admin, UUID noticeId, UUID revisionId) {
        Notice notice = load(noticeId);
        NoticeRevision revision = revisionRepository.findById(revisionId)
                .filter(found -> found.getNoticeId().equals(noticeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "이력을 찾을 수 없습니다."));

        saveRevision(notice, admin.email());
        notice.update(revision.getTitle(), revision.getContent(),
                revision.getCategory(), revision.isPinned(), null);

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, noticeId,
                "공지 내용 되돌림: %s (%s 시점으로)".formatted(
                        notice.getTitle(), revision.getCreatedAt()));
        return detailOf(notice);
    }

    /**
     * 예약 공개를 건다.
     *
     * <p>초안에만 걸 수 있다. 이미 공개된 공지는 예약할 것이 없다. 예약을 다시
     * 걸면 시각을 갈아끼운다.
     */
    @Transactional
    public NoticeDetail schedule(AdminPrincipal admin, UUID noticeId, OffsetDateTime publishAt) {
        Notice notice = load(noticeId);
        if (notice.getStatus() != ContentStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "초안 상태의 공지만 예약할 수 있습니다.");
        }
        if (publishAt == null || !publishAt.isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "예약 시각은 앞으로의 시각이어야 합니다.");
        }

        scheduleRepository.save(NoticeSchedule.builder()
                .noticeId(noticeId)
                .publishAt(publishAt)
                .createdByEmail(admin.email())
                .build());
        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, noticeId,
                "공지 예약 공개 설정: %s (%s)".formatted(notice.getTitle(), publishAt));
        return detailOf(notice);
    }

    @Transactional
    public NoticeDetail cancelSchedule(AdminPrincipal admin, UUID noticeId) {
        Notice notice = load(noticeId);
        scheduleRepository.findById(noticeId).ifPresent(schedule -> {
            scheduleRepository.delete(schedule);
            auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, noticeId,
                    "공지 예약 공개 취소: %s".formatted(notice.getTitle()));
        });
        return detailOf(notice);
    }

    @Transactional
    public void delete(AdminPrincipal admin, UUID noticeId) {
        Notice notice = load(noticeId);
        String title = notice.getTitle();
        repository.delete(notice);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, noticeId, "공지 삭제: %s".formatted(title));
    }

    private boolean contentChanges(Notice notice, UpdateRequest request) {
        return (request.title() != null && !request.title().equals(notice.getTitle()))
                || (request.content() != null && !request.content().equals(notice.getContent()))
                || (request.category() != null && request.category() != notice.getCategory())
                || (request.pinned() != null && request.pinned() != notice.isPinned());
    }

    private void saveRevision(Notice notice, String editorEmail) {
        revisionRepository.save(NoticeRevision.snapshotOf(notice, editorEmail));
        revisionRepository.deleteBeyondLatest(notice.getId(), REVISION_KEEP);
    }

    private NoticeDetail detailOf(Notice notice) {
        OffsetDateTime scheduledAt = scheduleRepository.findById(notice.getId())
                .map(NoticeSchedule::getPublishAt).orElse(null);
        return NoticeDetail.from(notice, scheduledAt);
    }

    private Notice load(UUID noticeId) {
        return repository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
    }
}
