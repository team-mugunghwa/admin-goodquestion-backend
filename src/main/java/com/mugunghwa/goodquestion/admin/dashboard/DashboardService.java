package com.mugunghwa.goodquestion.admin.dashboard;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.ContentStats;
import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.DashboardSummary;
import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.DailyPoint;
import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.RecentActivity;
import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.UserStats;
import com.mugunghwa.goodquestion.admin.dashboard.dto.DashboardDtos.WaitingInquiry;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogRepository;
import com.mugunghwa.goodquestion.admin.guide.GuideRepository;
import com.mugunghwa.goodquestion.admin.member.ChildRepository;
import com.mugunghwa.goodquestion.admin.member.DailyVisitRepository;
import com.mugunghwa.goodquestion.admin.member.StorySessionRepository;
import com.mugunghwa.goodquestion.admin.member.ParentRepository;
import com.mugunghwa.goodquestion.admin.notice.NoticeRepository;
import com.mugunghwa.goodquestion.admin.story.StoryRepository;
import com.mugunghwa.goodquestion.admin.story.StoryStatus;
import com.mugunghwa.goodquestion.admin.support.InquiryRepository;
import com.mugunghwa.goodquestion.admin.support.InquiryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 대시보드 집계.
 *
 * <p>모든 "오늘"은 한국 시간 기준이다. 서버가 UTC로 돌면 자정부터 오전 9시까지가 어제로
 * 집계되어, 아침에 대시보드를 여는 운영자에게 오늘 숫자가 계속 0으로 보인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /** 방문자 추이 그래프의 기간. 2주면 주 단위 리듬(주말에 오르는 패턴)이 보인다. */
    private static final int TREND_DAYS = 14;

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final StorySessionRepository storySessionRepository;
    private final DailyVisitRepository dailyVisitRepository;
    private final StoryRepository storyRepository;
    private final NoticeRepository noticeRepository;
    private final GuideRepository guideRepository;
    private final InquiryRepository inquiryRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardSummary summary() {
        LocalDate today = LocalDate.now(SERVICE_ZONE);
        OffsetDateTime startOfToday = today.atStartOfDay(SERVICE_ZONE).toOffsetDateTime();

        UserStats users = new UserStats(
                parentRepository.count(),
                childRepository.count(),
                dailyVisitRepository.countVisitors(today),
                parentRepository.countByCreatedAtGreaterThanEqual(startOfToday),
                childRepository.countByCreatedAtGreaterThanEqual(startOfToday),
                storySessionRepository.countByStartedAtGreaterThanEqual(startOfToday),
                storySessionRepository.countByStatus("IN_PROGRESS"));

        ContentStats content = new ContentStats(
                storyRepository.count(),
                storyRepository.countByStatus(StoryStatus.PUBLISHED),
                noticeRepository.countByStatus(ContentStatus.PUBLISHED),
                guideRepository.countByStatus(ContentStatus.PUBLISHED),
                inquiryRepository.countByStatus(InquiryStatus.PENDING));

        return new DashboardSummary(users, content, visitTrend(today), waitingInquiries(),
                recentActivities());
    }

    /**
     * 답변을 기다리는 문의 5건.
     *
     * <p>미답변 건수만으로는 급한 것이 있는지 알 수 없다. 3일 기다린 문의와 방금 들어온
     * 문의가 같은 숫자에 섞여 있어서, 지금까지는 문의 목록을 따로 열어야 알 수 있었다.
     *
     * <p>정렬은 리포지터리가 이미 PENDING을 오래된 순으로 준다. 맨 위가 가장 오래
     * 기다린 사람이라 그대로 위에서부터 처리하면 된다.
     */
    private List<WaitingInquiry> waitingInquiries() {
        return inquiryRepository.search(InquiryStatus.PENDING, null, "", PageRequest.of(0, 5))
                .getContent().stream()
                .map(inquiry -> new WaitingInquiry(inquiry.getId(), inquiry.getTitle(),
                        inquiry.getCategory(), inquiry.getCreatedAt()))
                .toList();
    }

    /**
     * 최근 2주 방문자 추이.
     *
     * <p>방문이 없던 날은 daily_visits에 행 자체가 없다. 그대로 내리면 그래프의 x축이
     * 건너뛰어 "쉬는 날"과 "데이터 없음"이 구분되지 않으므로 0으로 채워 준다.
     */
    private List<DailyPoint> visitTrend(LocalDate today) {
        LocalDate from = today.minusDays(TREND_DAYS - 1L);
        Map<LocalDate, Long> counted = dailyVisitRepository.countDailyFrom(from).stream()
                .collect(Collectors.toMap(row -> row.getDate(), row -> row.getVisitors()));

        return java.util.stream.IntStream.range(0, TREND_DAYS)
                .mapToObj(from::plusDays)
                .map(date -> new DailyPoint(date, counted.getOrDefault(date, 0L)))
                .toList();
    }

    /** 최근 관리자 조작 10건. "누가 방금 무엇을 바꿨는가"가 대시보드에서 가장 자주 찾는 정보다. */
    private List<RecentActivity> recentActivities() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .getContent().stream()
                .map(log -> new RecentActivity(log.getAdminEmail(), log.getAction().name(),
                        log.getTargetType(), log.getSummary(), log.getCreatedAt()))
                .toList();
    }
}
