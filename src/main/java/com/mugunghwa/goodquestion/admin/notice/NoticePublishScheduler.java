package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 예약 시각이 된 공지를 공개로 바꾼다.
 *
 * <p>1분마다 돈다. 예약은 "밤 12시 공개를 위해 밤에 접속하지 않으려고" 거는
 * 것이라 분 단위면 충분하다.
 *
 * <p>감사 로그에는 예약을 건 관리자의 이메일로 남긴다. 실행 주체는 시스템이지만
 * "누가 이 공개를 결정했는가"가 기록이 답해야 할 질문이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticePublishScheduler {

    private final NoticeRepository noticeRepository;
    private final NoticeScheduleRepository scheduleRepository;
    private final AuditLogger auditLogger;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void publishDue() {
        List<NoticeSchedule> due =
                scheduleRepository.findAllByPublishAtLessThanEqual(OffsetDateTime.now());

        for (NoticeSchedule schedule : due) {
            noticeRepository.findById(schedule.getNoticeId()).ifPresentOrElse(notice -> {
                // 예약 뒤에 손으로 먼저 공개했을 수 있다. 그 경우 예약만 걷어낸다.
                if (notice.getStatus() == ContentStatus.DRAFT) {
                    notice.update(null, null, null, null, ContentStatus.PUBLISHED);
                    auditLogger.logAnonymous(schedule.getCreatedByEmail(),
                            AuditAction.PUBLISH, "NOTICE",
                            "공지 예약 공개 실행: %s".formatted(notice.getTitle()));
                    log.info("공지 예약 공개: {} ({})", notice.getTitle(), notice.getId());
                }
                scheduleRepository.delete(schedule);
            }, () -> scheduleRepository.delete(schedule));
        }
    }
}
