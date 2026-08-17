package com.mugunghwa.goodquestion.admin.dashboard.dto;

import com.mugunghwa.goodquestion.admin.support.InquiryCategory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DashboardSummary(
            UserStats users,
            ContentStats content,
            /** 최근 2주 방문자 추이. 방문이 없던 날도 0으로 채워 보낸다. */
            List<DailyPoint> visitTrend,
            /** 답변을 기다리는 문의. 오래 기다린 순이라 맨 위가 가장 급하다. */
            List<WaitingInquiry> waitingInquiries,
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

    /**
     * 답변 대기 문의 한 줄.
     *
     * <p>대시보드의 미답변 건수는 "얼마나 남았는가"만 답한다. 3일 기다린 문의와 방금 들어온
     * 문의가 같은 숫자에 섞여 있어서, 급한 것이 있는지는 문의 목록을 열어 봐야 알 수 있었다.
     *
     * @param createdAt 접수 시각. 화면이 여기서 대기 시간을 계산한다
     */
    public record WaitingInquiry(
            UUID id,
            String title,
            InquiryCategory category,
            OffsetDateTime createdAt
    ) {
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
