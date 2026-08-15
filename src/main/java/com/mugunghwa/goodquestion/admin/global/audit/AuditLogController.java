package com.mugunghwa.goodquestion.admin.global.audit;

import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 감사 로그 조회. 목록만 있고 쓰기 API는 없다 - 기록은 각 서비스가 남긴다. */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository repository;

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        var result = StringUtils.hasText(targetType)
                ? repository.findAllByTargetTypeOrderByCreatedAtDesc(targetType, pageable)
                : repository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.of(result, AuditLogResponse::from);
    }

    public record AuditLogResponse(
            UUID id,
            UUID adminId,
            String adminEmail,
            AuditAction action,
            String targetType,
            String targetId,
            String summary,
            String ip,
            OffsetDateTime createdAt
    ) {
        static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(log.getId(), log.getAdminId(), log.getAdminEmail(),
                    log.getAction(), log.getTargetType(), log.getTargetId(),
                    log.getSummary(), log.getIp(), log.getCreatedAt());
        }
    }
}
