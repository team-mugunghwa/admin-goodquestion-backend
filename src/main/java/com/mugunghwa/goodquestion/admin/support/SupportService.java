package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.member.Parent;
import com.mugunghwa.goodquestion.admin.member.ParentRepository;
import com.mugunghwa.goodquestion.admin.notification.NotificationService;
import com.mugunghwa.goodquestion.admin.notification.NotificationType;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.AnswerRequest;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.AnswerResponse;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquiryDetail;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquirySummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 고객센터 관리.
 *
 * <p>이 서비스의 핵심은 답변 등록이다. 답변을 남기는 것만으로는 사용자가 알 수 없으므로
 * 같은 트랜잭션에서 알림을 함께 만들고, 커밋 후에 푸시가 나간다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private static final String TARGET_TYPE = "INQUIRY";

    /** 사용자 앱이 알림을 눌렀을 때 갈 화면. 프론트의 라우트와 맞춰야 한다. */
    private static final String INQUIRY_LINK_PATH = "/support/%s";

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository answerRepository;
    private final ParentRepository parentRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    public PageResponse<InquirySummary> list(InquiryStatus status, InquiryCategory category,
                                             String keyword, int page, int size) {
        Page<Inquiry> inquiries = inquiryRepository.search(status, category,
                StringUtils.hasText(keyword) ? keyword.trim() : "",
                PageRequest.of(page, Math.min(size, 100)));

        // 작성자 이름과 답변 여부를 문의마다 따로 조회하면 목록 한 번에 수십 건의
        // 쿼리가 나간다. 페이지 안의 것만 한 번씩 모아 온다.
        Map<UUID, Parent> parents = loadParents(inquiries.getContent());
        List<UUID> ids = inquiries.getContent().stream().map(Inquiry::getId).toList();
        var answered = ids.isEmpty() ? List.<InquiryAnswer>of()
                : answerRepository.findAllByInquiryIdIn(ids);
        var answeredIds = answered.stream().map(InquiryAnswer::getInquiryId).collect(Collectors.toSet());

        return PageResponse.of(inquiries, inquiry -> {
            Parent parent = parents.get(inquiry.getParentId());
            return new InquirySummary(inquiry.getId(), inquiry.getParentId(),
                    parent == null ? "(탈퇴한 사용자)" : parent.getName(),
                    parent == null ? null : parent.getEmail(),
                    inquiry.getCategory(), inquiry.getTitle(), inquiry.getStatus(),
                    answeredIds.contains(inquiry.getId()), inquiry.getAnsweredAt(),
                    inquiry.getCreatedAt());
        });
    }

    public InquiryDetail get(UUID inquiryId) {
        Inquiry inquiry = load(inquiryId);
        Parent parent = parentRepository.findById(inquiry.getParentId()).orElse(null);
        InquiryAnswer answer = answerRepository.findByInquiryId(inquiryId).orElse(null);
        return InquiryDetail.of(inquiry,
                parent == null ? "(탈퇴한 사용자)" : parent.getName(),
                parent == null ? null : parent.getEmail(),
                answer);
    }

    /**
     * 답변을 등록한다.
     *
     * <p>세 가지가 한 트랜잭션에서 일어난다: 답변 저장, 문의 상태를 답변 완료로 변경,
     * 사용자 알림 생성. 셋이 갈리면 "답변은 있는데 상태는 미답변"이나 "상태는 답변인데
     * 사용자는 모르는" 상태가 생긴다. 푸시만 커밋 뒤로 미룬다 - 벤더 호출이 트랜잭션
     * 안에 들어가면 DB 커넥션을 쥔 채 외부 응답을 기다리게 된다.
     */
    @Transactional
    public AnswerResponse answer(AdminPrincipal admin, UUID inquiryId, AnswerRequest request) {
        Inquiry inquiry = load(inquiryId);
        if (inquiry.isClosed()) {
            throw new BusinessException(ErrorCode.INQUIRY_CLOSED);
        }
        if (answerRepository.findByInquiryId(inquiryId).isPresent()) {
            // 수정은 PATCH로 받는다. 같은 엔드포인트가 등록과 수정을 겸하면
            // 이미 답변한 문의에 실수로 덮어쓰는 일이 조용히 일어난다.
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        InquiryAnswer answer = answerRepository.save(InquiryAnswer.builder()
                .inquiryId(inquiryId)
                .adminId(admin.id())
                .adminName(admin.name())
                .content(request.content())
                .build());
        inquiry.markAnswered();

        notifyAnswered(inquiry);
        auditLogger.log(admin, AuditAction.ANSWER, TARGET_TYPE, inquiryId,
                "문의 답변 등록: %s".formatted(inquiry.getTitle()));
        return AnswerResponse.from(answer);
    }

    /**
     * 답변 내용을 고친다.
     *
     * <p>알림을 다시 보내지 않는다. 오타를 고칠 때마다 사용자 기기에 푸시가 울리면
     * 알림 자체가 무시된다. 사용자는 문의 화면에서 최신 내용을 보게 된다.
     */
    @Transactional
    public AnswerResponse updateAnswer(AdminPrincipal admin, UUID inquiryId, AnswerRequest request) {
        load(inquiryId);
        InquiryAnswer answer = answerRepository.findByInquiryId(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        answer.updateContent(request.content(), admin.id(), admin.name());

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, inquiryId, "문의 답변 수정");
        return AnswerResponse.from(answer);
    }

    @Transactional
    public void close(AdminPrincipal admin, UUID inquiryId) {
        Inquiry inquiry = load(inquiryId);
        inquiry.close();
        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, inquiryId,
                "문의 종료: %s".formatted(inquiry.getTitle()));
    }

    @Transactional
    public void reopen(AdminPrincipal admin, UUID inquiryId) {
        Inquiry inquiry = load(inquiryId);
        inquiry.reopen();
        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, inquiryId,
                "문의 재개: %s".formatted(inquiry.getTitle()));
    }

    /** 미답변 건수. 대시보드가 쓴다. */
    public long countPending() {
        return inquiryRepository.countByStatus(InquiryStatus.PENDING);
    }

    private void notifyAnswered(Inquiry inquiry) {
        notificationService.notify(inquiry.getParentId(), NotificationType.INQUIRY_ANSWERED,
                "문의하신 내용에 답변이 등록되었습니다",
                // 문의 제목을 그대로 실어 어떤 문의인지 알 수 있게 한다. 답변 본문은
                // 싣지 않는다 - 길이를 예상할 수 없고, 알림 미리보기는 잠금 화면에도 뜬다.
                "\"%s\" 문의에 답변이 도착했어요. 눌러서 확인해 주세요.".formatted(inquiry.getTitle()),
                INQUIRY_LINK_PATH.formatted(inquiry.getId()));
    }

    private Map<UUID, Parent> loadParents(List<Inquiry> inquiries) {
        List<UUID> parentIds = inquiries.stream().map(Inquiry::getParentId).distinct().toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        return parentRepository.findAllByIdIn(parentIds).stream()
                .collect(Collectors.toMap(Parent::getId, Function.identity()));
    }

    private Inquiry load(UUID inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "문의를 찾을 수 없습니다."));
    }
}
