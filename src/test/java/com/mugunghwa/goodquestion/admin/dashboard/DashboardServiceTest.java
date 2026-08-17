package com.mugunghwa.goodquestion.admin.dashboard;

import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.DashboardSummary;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class DashboardServiceTest {

    @Autowired DashboardService dashboardService;
    @Autowired TestFixture fixture;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("오늘 방문자는 접속 횟수가 아니라 사람 수로 센다")
    void countsUniqueVisitors() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        UUID first = fixture.createParent("김보호자");
        UUID second = fixture.createParent("이보호자");
        // 한 사람이 세 번 들어온 것은 방문자 1명이다.
        insertVisit(first, today, 3);
        insertVisit(second, today, 1);

        DashboardSummary summary = dashboardService.summary();

        assertThat(summary.users().todayVisitors()).isEqualTo(2);
    }

    @Test
    @DisplayName("방문이 없던 날도 0으로 채워 2주치를 내린다")
    void fillsGapsInTrend() {
        DashboardSummary summary = dashboardService.summary();

        // 행이 없는 날을 건너뛰면 그래프에서 '쉬는 날'과 '데이터 없음'이 구분되지 않는다.
        assertThat(summary.visitTrend()).hasSize(14);
        assertThat(summary.visitTrend().getLast().date())
                .isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    @Test
    @DisplayName("가입자와 콘텐츠 집계가 함께 내려온다")
    void includesContentStats() {
        DashboardSummary summary = dashboardService.summary();

        assertThat(summary.users().totalParents()).isNotNegative();
        // 시드가 공지와 이용안내를 공개 상태로 넣어 둔다.
        assertThat(summary.content().publishedNotices()).isPositive();
        assertThat(summary.content().publishedGuides()).isPositive();
    }

    /**
     * 이 순서가 뒤집히면 대시보드가 급한 문의를 아래로 밀어낸다. 오래 기다린 사람이
     * 맨 위에 있어야 위에서부터 처리하는 것이 맞는 순서가 된다.
     */
    @Test
    @DisplayName("답변 대기 문의는 오래 기다린 순으로 내려온다")
    void waitingInquiriesOldestFirst() {
        UUID parentId = fixture.createParent("문의한보호자");
        insertInquiry(parentId, "사흘 전에 넣은 문의", 3);
        insertInquiry(parentId, "한 시간 전에 넣은 문의", 0);

        DashboardSummary summary = dashboardService.summary();

        assertThat(summary.waitingInquiries()).isNotEmpty();
        assertThat(summary.waitingInquiries().getFirst().title())
                .isEqualTo("사흘 전에 넣은 문의");
    }

    @Test
    @DisplayName("답변 대기 문의는 5건까지만 내린다")
    void waitingInquiriesAreCapped() {
        UUID parentId = fixture.createParent("문의많은보호자");
        for (int i = 0; i < 7; i++) {
            insertInquiry(parentId, "문의 %d".formatted(i), i);
        }

        DashboardSummary summary = dashboardService.summary();

        assertThat(summary.waitingInquiries()).hasSizeLessThanOrEqualTo(5);
    }

    /** 답변 대기 상태로 넣는다. created_at은 DB 기본값이라 넣은 뒤 따로 당긴다. */
    private void insertInquiry(UUID parentId, String title, int daysAgo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into inquiries (id, parent_id, category, title, content, status)
                values (?, ?, 'ETC', ?, '내용', 'PENDING')
                """, id, parentId, title);
        jdbcTemplate.update(
                "update inquiries set created_at = now() - make_interval(days => ?) where id = ?",
                daysAgo, id);
    }

    private void insertVisit(UUID parentId, LocalDate date, int count) {
        jdbcTemplate.update("""
                insert into daily_visits (parent_id, visit_date, visit_count)
                values (?, ?, ?)
                """, parentId, date, count);
    }
}
