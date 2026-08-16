package com.mugunghwa.goodquestion.admin.global.audit;

import com.mugunghwa.goodquestion.admin.global.audit.AuditLogQueryService.Csv;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogQueryService.Filter;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 감사 로그 필터와 CSV 내보내기.
 *
 * <p>{@code @Transactional} 로 감싼다. 로그를 직접 쌓아 놓고 조회하는 테스트라
 * 서로의 데이터가 보이면 개수 검증이 흔들린다.
 */
@IntegrationTest
@Transactional
class AuditLogQueryServiceTest {

    @Autowired
    private AuditLogQueryService queryService;

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private TestFixture fixture;

    private AdminPrincipal admin;
    private AdminPrincipal other;

    private static final Filter EVERYTHING = new Filter(null, null, null, null, null);

    @BeforeEach
    void seed() {
        admin = fixture.createAdmin();
        other = fixture.createAdmin();
        auditLogger.log(admin, AuditAction.DELETE, "NOTICE", "n-1", "공지 삭제");
        auditLogger.log(admin, AuditAction.READ_DATA, "DATABASE", "children", "개인정보 테이블 조회");
        auditLogger.log(other, AuditAction.DELETE, "STORY", "s-1", "이야기, \"괜찮아\" 삭제");
    }

    @Test
    @DisplayName("조작 종류로 거른다")
    void filtersByAction() {
        Page<AuditLog> result = queryService.search(
                new Filter(null, AuditAction.READ_DATA, null, null, null), 0, 20);

        assertThat(result.getContent())
                .allSatisfy(log -> assertThat(log.getAction()).isEqualTo(AuditAction.READ_DATA));
        assertThat(result.getContent())
                .anySatisfy(log -> assertThat(log.getTargetId()).isEqualTo("children"));
    }

    @Test
    @DisplayName("관리자 이메일 부분 일치로 거른다")
    void filtersByAdminEmail() {
        // fixture 이메일은 fixture-<나노초>@goodquestion.kr 이라 이 관리자만 걸리게
        // 하려면 고유한 앞부분을 쓴다.
        String unique = admin.email().substring(0, admin.email().indexOf('@'));

        Page<AuditLog> result = queryService.search(
                new Filter(null, null, unique, null, null), 0, 20);

        assertThat(result.getContent()).hasSize(2)
                .allSatisfy(log -> assertThat(log.getAdminEmail()).isEqualTo(admin.email()));
    }

    @Test
    @DisplayName("기간의 끝 날짜는 그날까지 포함한다")
    void endDateIsInclusive() {
        // 오늘 쌓은 로그가 "오늘 ~ 오늘" 범위에 걸려야 한다. 배타적 끝으로 처리하면
        // 같은 날을 고른 필터가 항상 0건이 되어 고장으로 보인다.
        LocalDate today = LocalDate.now();
        Page<AuditLog> result = queryService.search(
                new Filter(null, null, null, today, today), 0, 20);

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("시작 날짜가 끝보다 늦으면 거절한다")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> queryService.search(
                new Filter(null, null, null,
                        LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1)), 0, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("시작 날짜");
    }

    @Test
    @DisplayName("CSV 에 걸린 행이 전부 들어가고 특수한 값은 감싸진다")
    void rendersCsv() {
        Csv csv = queryService.exportCsv(admin, EVERYTHING);

        assertThat(csv.fileName()).startsWith("audit_logs_").endsWith(".csv");
        // 엑셀이 한글을 깨뜨리지 않도록 BOM 으로 시작해야 한다.
        assertThat(csv.content()).startsWith("\uFEFF");
        assertThat(csv.content()).contains("시각,관리자,조작,대상,대상 ID,내용,IP");
        // 콤마와 따옴표가 든 summary 는 CSV 규칙대로 감싸진다.
        assertThat(csv.content()).contains("\"이야기, \"\"괜찮아\"\" 삭제\"");
    }

    @Test
    @DisplayName("내보내기 자체가 감사 로그에 남는다")
    void exportLeavesAuditTrail() {
        long before = queryService.search(
                new Filter("AUDIT_LOG", AuditAction.READ_DATA, null, null, null), 0, 1)
                .getTotalElements();

        queryService.exportCsv(admin, EVERYTHING);

        Page<AuditLog> after = queryService.search(
                new Filter("AUDIT_LOG", AuditAction.READ_DATA, null, null, null), 0, 5);
        assertThat(after.getTotalElements()).isEqualTo(before + 1);
        assertThat(after.getContent().getFirst().getSummary()).contains("내보내기");
    }

    @Test
    @DisplayName("CSV 는 내보내는 순간의 기록만 담는다")
    void exportRowLandsAfterSnapshot() {
        // 내보내기 기록이 파일 안에까지 들어가면 뽑을 때마다 줄 수가 하나씩 달라진다.
        Csv csv = queryService.exportCsv(admin, EVERYTHING);

        assertThat(csv.content()).doesNotContain("내보내기");
    }
}
