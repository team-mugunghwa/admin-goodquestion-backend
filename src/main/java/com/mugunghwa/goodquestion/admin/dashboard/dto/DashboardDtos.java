package com.mugunghwa.goodquestion.admin.dashboard.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSummary(
            UserStats users,
            ContentStats content,
            /** 최근 2주 방문자 추이. 방문이 없던 날도 0으로 채워 보낸다. */
            List<DailyPoint> visitTrend,
            List<RecentActivity> recentActivities
    ) {
    }

    /**
     * @param todayVisitors   오늘 다녀간 순 방문자 수. 접속 횟수가 아니다.
     * @param activeSessions  지금 진행 중인 학습 세션. 이야기를 하다 만 것도 포함된다.
     */
    public record UserStats(
            long totalParents,
            long totalChildren,
            long todayVisitors,
            long todayNewParents,
            long todayNewChildren,
            long todaySessions,
            long activeSessions
    ) {
    }

    public record ContentStats(
            long totalStories,
            long publishedStories,
            long publishedNotices,
            long publishedGuides,
            /** 미답변 문의. 이 숫자가 대시보드에서 가장 먼저 봐야 할 값이다. */
            long pendingInquiries
    ) {
    }

    public record DailyPoint(LocalDate date, long value) {
    }

    public record RecentActivity(
            String adminEmail,
            String action,
            String targetType,
            String summary,
            OffsetDateTime createdAt
    ) {
    }
}
