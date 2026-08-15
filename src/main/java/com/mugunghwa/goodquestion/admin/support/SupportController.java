package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.AnswerRequest;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.AnswerResponse;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquiryDetail;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquirySummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 고객센터 관리.
 *
 * <p>문의 생성 API가 없다. 문의는 사용자 앱만 만든다.
 */
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping
    public PageResponse<InquirySummary> list(@RequestParam(required = false) InquiryStatus status,
                                             @RequestParam(required = false) InquiryCategory category,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return supportService.list(status, category, keyword, page, size);
    }

    @GetMapping("/{inquiryId}")
    public InquiryDetail get(@PathVariable UUID inquiryId) {
        return supportService.get(inquiryId);
    }

    /** 답변 등록. 사용자 알림이 함께 만들어지고 커밋 후 푸시가 나간다. */
    @PostMapping("/{inquiryId}/answer")
    public AnswerResponse answer(@CurrentAdmin AdminPrincipal admin,
                                 @PathVariable UUID inquiryId,
                                 @Valid @RequestBody AnswerRequest request) {
        return supportService.answer(admin, inquiryId, request);
    }

    /** 답변 수정. 알림은 다시 보내지 않는다. */
    @PatchMapping("/{inquiryId}/answer")
    public AnswerResponse updateAnswer(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID inquiryId,
                                       @Valid @RequestBody AnswerRequest request) {
        return supportService.updateAnswer(admin, inquiryId, request);
    }

    @PostMapping("/{inquiryId}/close")
    public ResponseEntity<Void> close(@CurrentAdmin AdminPrincipal admin,
                                      @PathVariable UUID inquiryId) {
        supportService.close(admin, inquiryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{inquiryId}/reopen")
    public ResponseEntity<Void> reopen(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID inquiryId) {
        supportService.reopen(admin, inquiryId);
        return ResponseEntity.noContent().build();
    }
}
