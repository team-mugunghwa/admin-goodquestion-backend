package com.mugunghwa.goodquestion.admin.notification;

import com.mugunghwa.goodquestion.admin.notification.NotificationService.NotificationCreatedEvent;
import com.mugunghwa.goodquestion.admin.notification.push.PushMessage;
import com.mugunghwa.goodquestion.admin.notification.push.PushResult;
import com.mugunghwa.goodquestion.admin.notification.push.PushSender;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 발송 결과에 따라 토큰을 어떻게 처리하는지 본다.
 *
 * <p>커밋 후 비동기로 도는 배선(PushDispatcher)까지 함께 검증하려면 다른 스레드가
 * 끝나기를 기다려야 해서 테스트가 시간에 의존하게 된다. 그 배선은 스프링이 보장하는
 * 부분이므로 여기서는 실제 일을 하는 {@link PushDelivery}만 직접 부른다.
 */
@IntegrationTest
class PushDeliveryTest {

    @Autowired PushDelivery pushDelivery;
    @Autowired DeviceTokenRepository deviceTokenRepository;
    @Autowired TestFixture fixture;

    @MockitoBean PushSender pushSender;

    @Test
    @DisplayName("벤더가 토큰을 거절하면 그 토큰을 비활성으로 바꾼다")
    void disablesRejectedToken() {
        UUID parentId = fixture.createParent("김보호자");
        fixture.createDeviceToken(parentId, "dead-token-%s".formatted(System.nanoTime()));
        given(pushSender.send(anyString(), any(PushMessage.class))).willReturn(PushResult.TOKEN_INVALID);

        pushDelivery.deliver(new NotificationCreatedEvent(parentId, "제목", "본문", "/support/1"));

        // 죽은 토큰을 남겨 두면 발송할 때마다 같은 실패를 되풀이한다.
        assertThat(deviceTokenRepository.findAllByParentIdAndDisabledAtIsNull(parentId)).isEmpty();
    }

    @Test
    @DisplayName("벤더 장애로 실패한 경우에는 토큰을 건드리지 않는다")
    void keepsTokenOnTransientFailure() {
        UUID parentId = fixture.createParent("이보호자");
        fixture.createDeviceToken(parentId, "live-token-%s".formatted(System.nanoTime()));
        given(pushSender.send(anyString(), any(PushMessage.class))).willReturn(PushResult.FAILED);

        pushDelivery.deliver(new NotificationCreatedEvent(parentId, "제목", "본문", null));

        // 벤더가 잠시 죽은 것뿐인데 토큰을 지우면 그 사용자는 영영 푸시를 못 받는다.
        List<DeviceToken> tokens = deviceTokenRepository.findAllByParentIdAndDisabledAtIsNull(parentId);
        assertThat(tokens).hasSize(1);
    }
}
