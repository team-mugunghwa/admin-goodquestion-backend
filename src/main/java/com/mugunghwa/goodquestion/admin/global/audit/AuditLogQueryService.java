package com.mugunghwa.goodquestion.admin.global.audit;

import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

/**
 * 감사 로그 조회와 내보내기.
 *
 * <p>필터가 늘면서 컨트롤러가 리포지터리를 직접 부르던 구조로는 좁아졌다.
 * 기간 해석(날짜 -> 서울 기준 시각 범위)과 CSV 조립을 여기에 모은다.
 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    /** 화면과 대시보드가 쓰는 서비스 기준 시간대. 날짜 필터도 같은 기준으로 해석한다. */
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 내보내기 상한. 상한 없이 전부 내리면 언젠가 요청 하나가 메모리를 다 쓴다.
     * 넘으면 자르지 않고 거절한다 - 잘린 CSV 는 "전부 받았다"로 읽혀서 더 위험하다.
     */
    static final int EXPORT_MAX_ROWS = 10_000;

    /** 기간을 안 주면 사실상 전체를 뜻하는 범위. */
    private static final LocalDate OLDEST = LocalDate.of(2000, 1, 1);

    private static final DateTimeFormatter CSV_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository repository;
    private final AuditLogger auditLogger;

    public record Filter(String targetType, AuditAction action, String adminEmail,
                         LocalDate from, LocalDate to) {
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(Filter filter, int page, int size) {
        return query(filter, PageRequest.of(page, Math.min(size, 100)));
    }

    /**
     * 필터에 걸린 로그 전체를 CSV 로 만든다.
     *
     * <p>이 조회 자체를 감사 로그에 남긴다. 화면에서 한 장씩 보는 것과 파일로
     * 시스템 밖으로 나가는 것은 무게가 다르고, 나중에 "언제 누가 뽑아갔는지"를
     * 물을 수 있어야 한다.
     */
    @Transactional
    public Csv exportCsv(AdminPrincipal admin, Filter filter) {
        Page<AuditLog> result =
                query(filter, PageRequest.of(0, EXPORT_MAX_ROWS));
        if (result.getTotalElements() > EXPORT_MAX_ROWS) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "%,d건이 걸렸습니다. 내보내기는 한 번에 %,d건까지입니다. 기간을 좁혀 주세요."
                            .formatted(result.getTotalElements(), EXPORT_MAX_ROWS));
        }

        auditLogger.log(admin, AuditAction.READ_DATA, "AUDIT_LOG", (String) null,
                "감사 로그 내보내기: %d건".formatted(result.getNumberOfElements()));

        return new Csv(fileName(), render(result.getContent()));
    }

    public record Csv(String fileName, String content) {
    }

    private Page<AuditLog> query(Filter filter, PageRequest pageable) {
        LocalDate from = filter.from() == null ? OLDEST : filter.from();
        // 끝 날짜는 그날까지 포함이다. 화면에서 "8월 1일 ~ 8월 1일"을 골랐는데
        // 0건이 나오면 필터가 고장 난 것으로 보인다.
        LocalDate toExclusive = (filter.to() == null ? LocalDate.of(2100, 1, 1) : filter.to())
                .plusDays(1);
        if (!from.isBefore(toExclusive)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "시작 날짜가 끝 날짜보다 늦습니다.");
        }

        return repository.search(
                StringUtils.hasText(filter.targetType()) ? filter.targetType().trim() : "",
                StringUtils.hasText(filter.adminEmail()) ? filter.adminEmail().trim() : "",
                filter.action() == null
                        ? EnumSet.allOf(AuditAction.class)
                        : EnumSet.of(filter.action()),
                atStartOfDay(from),
                atStartOfDay(toExclusive),
                pageable);
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        return date.atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
    }

    private String fileName() {
        return "audit_logs_%s.csv".formatted(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
                        .format(OffsetDateTime.now(SERVICE_ZONE)));
    }

    private String render(List<AuditLog> logs) {
        StringBuilder csv = new StringBuilder();
        // 엑셀은 BOM 이 없으면 UTF-8 한글을 깨뜨린다. 이 파일의 최종 목적지는
        // 거의 항상 엑셀이다.
        csv.append('\uFEFF');
        csv.append("시각,관리자,조작,대상,대상 ID,내용,IP\n");
        for (AuditLog log : logs) {
            csv.append(String.join(",",
                            field(log.getCreatedAt() == null ? "" :
                                    CSV_TIME.format(log.getCreatedAt().atZoneSameInstant(SERVICE_ZONE))),
                            field(log.getAdminEmail()),
                            field(log.getAction().name()),
                            field(log.getTargetType()),
                            field(log.getTargetId()),
                            field(log.getSummary()),
                            field(log.getIp())))
                    .append('\n');
        }
        return csv.toString();
    }

    /** 콤마, 따옴표, 줄바꿈이 든 값을 감싼다. summary 는 자유 문장이라 다 들어올 수 있다. */
    private String field(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
