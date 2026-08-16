package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.NoticeDetail;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.UpdateRequest;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 공지 안전장치 - 수정 이력, 되돌리기, 예약 공개. */
@IntegrationTest
@Transactional
class ContentSafetyTest {

    @Autowired NoticeService noticeService;
    @Autowired NoticePublishScheduler scheduler;
    @Autowired NoticeScheduleRepository scheduleRepository;
    @Autowired TestFixture fixture;

    private AdminPrincipal admin;
    private UUID noticeId;

    @BeforeEach
    void seed() {
        admin = fixture.createAdmin();
        noticeId = noticeService.create(admin, new CreateRequest(
                "점검 안내", "8월 20일 점검합니다.", NoticeCategory.GENERAL, false, null)).id();
    }

    @Test
    @DisplayName("내용을 고치면 바꾸기 전 내용이 이력으로 남는다")
    void updateLeavesRevision() {
        noticeService.update(admin, noticeId,
                new UpdateRequest("점검 안내 (수정)", null, null, null, null));

        List<NoticeRevision> revisions = noticeService.revisions(noticeId);
        assertThat(revisions).hasSize(1);
        assertThat(revisions.getFirst().getTitle()).isEqualTo("점검 안내");
        assertThat(revisions.getFirst().getEditedByEmail()).isEqualTo(admin.email());
    }

    @Test
    @DisplayName("상태만 바꾼 저장은 이력을 남기지 않는다")
    void statusOnlyChangeLeavesNoRevision() {
        // 공개/비공개 전환까지 남기면 같은 내용의 이력이 쌓인다.
        noticeService.update(admin, noticeId,
                new UpdateRequest(null, null, null, null, ContentStatus.PUBLISHED));

        assertThat(noticeService.revisions(noticeId)).isEmpty();
    }

    @Test
    @DisplayName("되돌리면 내용이 그 시점으로 돌아가고, 지금 내용도 이력으로 남는다")
    void revertRestoresAndKeepsCurrent() {
        noticeService.update(admin, noticeId,
                new UpdateRequest("점검 안내 v2", "본문 v2", null, null, null));
        UUID revisionId = noticeService.revisions(noticeId).getFirst().getId();

        NoticeDetail reverted = noticeService.revert(admin, noticeId, revisionId);

        assertThat(reverted.title()).isEqualTo("점검 안내");
        // "아까가 낫네"가 되면 다시 돌아올 길이 있어야 한다.
        assertThat(noticeService.revisions(noticeId))
                .anySatisfy(r -> assertThat(r.getTitle()).isEqualTo("점검 안내 v2"));
    }

    @Test
    @DisplayName("되돌리기는 공개 여부를 건드리지 않는다")
    void revertKeepsStatus() {
        noticeService.update(admin, noticeId,
                new UpdateRequest("v2", null, null, null, ContentStatus.PUBLISHED));
        UUID revisionId = noticeService.revisions(noticeId).getFirst().getId();

        NoticeDetail reverted = noticeService.revert(admin, noticeId, revisionId);

        assertThat(reverted.status()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    @DisplayName("다른 공지의 이력으로는 되돌릴 수 없다")
    void rejectsForeignRevision() {
        noticeService.update(admin, noticeId,
                new UpdateRequest("v2", null, null, null, null));
        UUID revisionId = noticeService.revisions(noticeId).getFirst().getId();

        UUID otherId = noticeService.create(admin, new CreateRequest(
                "다른 공지", "본문", NoticeCategory.GENERAL, false, null)).id();

        assertThatThrownBy(() -> noticeService.revert(admin, otherId, revisionId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이력은 공지당 최신 20개까지만 남는다")
    void capsRevisions() {
        for (int i = 1; i <= 25; i++) {
            noticeService.update(admin, noticeId,
                    new UpdateRequest("제목 " + i, null, null, null, null));
        }

        List<NoticeRevision> revisions = noticeService.revisions(noticeId);
        assertThat(revisions).hasSize(NoticeService.REVISION_KEEP);
        // 가장 오래된 원본("점검 안내")은 잘려 나갔다.
        assertThat(revisions).noneSatisfy(
                r -> assertThat(r.getTitle()).isEqualTo("점검 안내"));
    }

    @Test
    @DisplayName("초안에 예약을 걸면 상세에 예약 시각이 실린다")
    void scheduleShowsInDetail() {
        OffsetDateTime at = OffsetDateTime.now().plusHours(3);
        NoticeDetail detail = noticeService.schedule(admin, noticeId, at);

        assertThat(detail.scheduledPublishAt()).isEqualTo(at);
        assertThat(noticeService.get(noticeId).scheduledPublishAt()).isEqualTo(at);
    }

    @Test
    @DisplayName("공개된 공지나 지난 시각으로는 예약할 수 없다")
    void rejectsBadSchedules() {
        assertThatThrownBy(() -> noticeService.schedule(admin, noticeId,
                OffsetDateTime.now().minusMinutes(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("앞으로의 시각");

        noticeService.update(admin, noticeId,
                new UpdateRequest(null, null, null, null, ContentStatus.PUBLISHED));
        assertThatThrownBy(() -> noticeService.schedule(admin, noticeId,
                OffsetDateTime.now().plusHours(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("초안");
    }

    @Test
    @DisplayName("시각이 되면 스케줄러가 공개로 바꾸고 예약을 지운다")
    void schedulerPublishesDue() {
        noticeService.schedule(admin, noticeId, OffsetDateTime.now().plusHours(1));
        // 시각을 기다리는 대신 예약을 과거로 옮겨 "때가 됐다"를 만든다.
        NoticeSchedule schedule = scheduleRepository.findById(noticeId).orElseThrow();
        scheduleRepository.save(NoticeSchedule.builder()
                .noticeId(noticeId)
                .publishAt(OffsetDateTime.now().minusMinutes(1))
                .createdByEmail(schedule.getCreatedByEmail())
                .build());

        scheduler.publishDue();

        NoticeDetail detail = noticeService.get(noticeId);
        assertThat(detail.status()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(detail.publishedAt()).isNotNull();
        assertThat(detail.scheduledPublishAt()).isNull();
    }

    @Test
    @DisplayName("예약 뒤 손으로 먼저 공개했으면 스케줄러는 예약만 걷어낸다")
    void schedulerSkipsAlreadyPublished() {
        noticeService.schedule(admin, noticeId, OffsetDateTime.now().plusHours(1));
        noticeService.update(admin, noticeId,
                new UpdateRequest(null, null, null, null, ContentStatus.PUBLISHED));
        OffsetDateTime publishedAt = noticeService.get(noticeId).publishedAt();

        NoticeSchedule schedule = scheduleRepository.findById(noticeId).orElseThrow();
        scheduleRepository.save(NoticeSchedule.builder()
                .noticeId(noticeId)
                .publishAt(OffsetDateTime.now().minusMinutes(1))
                .createdByEmail(schedule.getCreatedByEmail())
                .build());

        scheduler.publishDue();

        // 공개 시각이 다시 찍히지 않았고 예약은 사라졌다.
        assertThat(noticeService.get(noticeId).publishedAt()).isEqualTo(publishedAt);
        assertThat(scheduleRepository.findById(noticeId)).isEmpty();
    }

    @Test
    @DisplayName("예약을 취소하면 상세에서 사라진다")
    void cancelSchedule() {
        noticeService.schedule(admin, noticeId, OffsetDateTime.now().plusHours(1));
        NoticeDetail detail = noticeService.cancelSchedule(admin, noticeId);
        assertThat(detail.scheduledPublishAt()).isNull();
    }
}
