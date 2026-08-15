package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.notification.NotificationRepository;
import com.mugunghwa.goodquestion.admin.notification.NotificationType;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.AnswerRequest;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquiryDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class SupportServiceTest {

    @Autowired SupportService supportService;
    @Autowired InquiryRepository inquiryRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired TestFixture fixture;

    private AdminPrincipal admin;
    private UUID parentId;
    private UUID inquiryId;

    @BeforeEach
    void setUp() {
        admin = fixture.createAdmin();
        parentId = fixture.createParent("김보호자");
        inquiryId = fixture.createInquiry(parentId, "로그인이 안 돼요");
    }

    @Test
    @DisplayName("답변을 등록하면 문의가 답변 완료로 바뀌고 사용자 알림이 함께 생긴다")
    void answerCreatesNotification() {
        supportService.answer(admin, inquiryId, new AnswerRequest("확인 후 조치했습니다."));

        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow();
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getAnsweredAt()).isNotNull();

        // 알림이 없으면 푸시가 막혀 있을 때 사용자가 답변을 알 방법이 사라진다.
        var notifications = notificationRepository.findAll().stream()
                .filter(n -> n.getParentId().equals(parentId))
                .toList();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.INQUIRY_ANSWERED);
        assertThat(notifications.getFirst().getLinkPath()).isEqualTo("/support/" + inquiryId);
    }

    @Test
    @DisplayName("이미 답변한 문의에 다시 등록하면 거절한다")
    void rejectsDuplicateAnswer() {
        supportService.answer(admin, inquiryId, new AnswerRequest("첫 답변"));

        assertThatThrownBy(() -> supportService.answer(admin, inquiryId, new AnswerRequest("덮어쓰기")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANSWER_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("답변을 수정해도 알림을 다시 보내지 않는다")
    void updateDoesNotNotifyAgain() {
        supportService.answer(admin, inquiryId, new AnswerRequest("첫 답변"));
        supportService.updateAnswer(admin, inquiryId, new AnswerRequest("오타를 고친 답변"));

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getParentId().equals(parentId))
                .count();
        // 오타를 고칠 때마다 푸시가 울리면 알림 자체가 무시된다.
        assertThat(count).isEqualTo(1);

        InquiryDetail detail = supportService.get(inquiryId);
        assertThat(detail.answer().content()).isEqualTo("오타를 고친 답변");
    }

    @Test
    @DisplayName("종료된 문의에는 답변할 수 없다")
    void cannotAnswerClosedInquiry() {
        supportService.close(admin, inquiryId);

        assertThatThrownBy(() -> supportService.answer(admin, inquiryId, new AnswerRequest("답변")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_CLOSED);
    }

    @Test
    @DisplayName("미답변 문의가 답변된 문의보다 먼저 나온다")
    void pendingFirst() {
        supportService.answer(admin, inquiryId, new AnswerRequest("답변"));
        UUID pendingId = fixture.createInquiry(parentId, "별가루가 안 들어와요");

        var page = supportService.list(null, null, null, 0, 50);
        var ids = page.content().stream().map(s -> s.id()).toList();
        assertThat(ids.indexOf(pendingId)).isLessThan(ids.indexOf(inquiryId));
    }
}
