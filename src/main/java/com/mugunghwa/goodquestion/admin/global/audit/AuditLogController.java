package com.mugunghwa.goodquestion.admin.global.audit;

import com.mugunghwa.goodquestion.admin.global.audit.AuditLogQueryService.Filter;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 감사 로그 조회. 목록과 내보내기만 있고 쓰기 API는 없다 - 기록은 각 서비스가 남긴다. */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService queryService;

    /**
     * 목록.
     *
     * @param targetType 대상 종류. NOTICE, DATABASE 같은 리소스 이름
     * @param action     조작 종류. LOGIN, DELETE, READ_DATA 등
     * @param adminEmail 관리자 이메일. 부분 일치
     * @param from       이 날짜부터(포함). 서울 기준
     * @param to         이 날짜까지(포함). 서울 기준
     */
    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(
                queryService.search(new Filter(targetType, action, adminEmail, from, to), page, size),
                AuditLogResponse::from);
    }

    /**
     * CSV 내보내기. 목록과 같은 필터를 받는다.
     *
     * <p>내보내기 자체가 감사 로그에 남는다. 파일로 시스템 밖으로 나가는 것은
     * 화면에서 한 장씩 보는 것과 무게가 다르다.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @CurrentAdmin AdminPrincipal admin,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String adminEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var csv = queryService.exportCsv(
                admin, new Filter(targetType, action, adminEmail, from, to));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"".formatted(csv.fileName()))
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.content().getBytes(StandardCharsets.UTF_8));
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
