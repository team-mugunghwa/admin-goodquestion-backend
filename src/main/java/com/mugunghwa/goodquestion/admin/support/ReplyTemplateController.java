package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 자주 쓰는 답변 템플릿.
 *
 * <p>팀이 공유하는 자산이라 만든 사람만 고칠 수 있는 제한을 두지 않는다.
 * 대신 만들고 고치고 지우는 것을 감사 로그에 남긴다.
 *
 * <p>로직이 CRUD 뿐이라 서비스 계층 없이 컨트롤러가 리포지터리를 직접 쓴다.
 */
@RestController
@RequestMapping("/api/admin/reply-templates")
@RequiredArgsConstructor
public class ReplyTemplateController {

    private static final String TARGET_TYPE = "REPLY_TEMPLATE";

    private final ReplyTemplateRepository repository;
    private final AuditLogger auditLogger;

    @GetMapping
    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(TemplateResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public TemplateResponse create(@CurrentAdmin AdminPrincipal admin,
                                   @Valid @RequestBody TemplateRequest request) {
        ReplyTemplate template = repository.save(ReplyTemplate.builder()
                .title(request.title().trim())
                .body(request.body())
                .build());
        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, template.getId(),
                "답변 템플릿 생성: %s".formatted(template.getTitle()));
        return TemplateResponse.from(template);
    }

    @PatchMapping("/{templateId}")
    @Transactional
    public TemplateResponse update(@CurrentAdmin AdminPrincipal admin,
                                   @PathVariable UUID templateId,
                                   @Valid @RequestBody TemplateRequest request) {
        ReplyTemplate template = load(templateId);
        template.update(request.title().trim(), request.body());
        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, templateId,
                "답변 템플릿 수정: %s".formatted(template.getTitle()));
        return TemplateResponse.from(template);
    }

    @DeleteMapping("/{templateId}")
    @Transactional
    public ResponseEntity<Void> delete(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID templateId) {
        ReplyTemplate template = load(templateId);
        repository.delete(template);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, templateId,
                "답변 템플릿 삭제: %s".formatted(template.getTitle()));
        return ResponseEntity.noContent().build();
    }

    private ReplyTemplate load(UUID templateId) {
        return repository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "템플릿을 찾을 수 없습니다."));
    }

    public record TemplateRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank String body
    ) {
    }

    public record TemplateResponse(
            UUID id,
            String title,
            String body,
            OffsetDateTime updatedAt
    ) {
        static TemplateResponse from(ReplyTemplate template) {
            return new TemplateResponse(template.getId(), template.getTitle(),
                    template.getBody(), template.getUpdatedAt());
        }
    }
}
