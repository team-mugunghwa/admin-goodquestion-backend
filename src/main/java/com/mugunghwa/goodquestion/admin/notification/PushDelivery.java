package com.mugunghwa.goodquestion.admin.notification;

import com.mugunghwa.goodquestion.admin.notification.NotificationService.NotificationCreatedEvent;
import com.mugunghwa.goodquestion.admin.notification.push.PushMessage;
import com.mugunghwa.goodquestion.admin.notification.push.PushResult;
import com.mugunghwa.goodquestion.admin.notification.push.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 한 사용자의 등록된 기기 전부에 푸시를 보내고, 벤더가 거절한 토큰을 정리한다.
 *
 * <p>{@link PushDispatcher}와 나눠 둔 이유는 프록시 때문이다. 한 클래스에 두면
 * {@code @Async}가 걸린 메서드를 부르는 순간 즉시 반환되어, 발송 결과에 따라 토큰을
 * 어떻게 처리하는지 확인할 방법이 "잠깐 기다렸다 보는 것"밖에 남지 않는다.
 * 언제 보내는가(디스패처)와 무엇을 하는가(여기)를 나누면 후자를 그냥 부를 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDelivery {

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;

    @Transactional
    public void deliver(NotificationCreatedEvent event) {
        List<DeviceToken> tokens =
                deviceTokenRepository.findAllByParentIdAndDisabledAtIsNull(event.parentId());
        if (tokens.isEmpty()) {
            // 기기를 등록한 적 없는 사용자다. 알림함에는 남아 있으므로 문제가 아니다.
            log.debug("등록된 기기가 없어 푸시를 보내지 않는다. parentId={}", event.parentId());
            return;
        }

        PushMessage message = PushMessage.of(event.title(), event.body(), event.linkPath());
        for (DeviceToken token : tokens) {
            PushResult result = pushSender.send(token.getToken(), message);
            if (result == PushResult.TOKEN_INVALID) {
                // 죽은 토큰을 남겨 두면 발송할 때마다 같은 실패를 되풀이한다.
                token.disable();
            } else if (result == PushResult.FAILED) {
                // 벤더가 잠시 죽은 것뿐이므로 토큰은 건드리지 않는다. 다음 알림 때 다시 시도된다.
                log.warn("푸시 발송 실패. parentId={} deviceTokenId={}", event.parentId(), token.getId());
            }
        }
    }
}
