package com.mugunghwa.goodquestion.admin.notification;

import com.mugunghwa.goodquestion.admin.notification.NotificationService.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림이 저장되고 커밋된 뒤 푸시 발송을 시작한다. 실제 발송은 {@link PushDelivery}가 한다.
 *
 * <p>{@code AFTER_COMMIT}인 이유: 트랜잭션 안에서 보내면 뒤에서 롤백이 나도 푸시는
 * 이미 나간 뒤라 되돌릴 수 없다. 사용자는 "답변이 등록됐다"는 알림을 받고 들어왔는데
 * 아무것도 없는 상태를 보게 된다.
 *
 * <p>{@code @Async}인 이유: 벤더 호출은 수백 ms에서 타임아웃까지 걸리고 기기가 여러
 * 대면 그만큼 곱해진다. 관리자가 답변 등록 버튼을 누르고 그 시간을 기다릴 이유가 없다.
 *
 * <p>여기서 난 예외는 관리자 응답에 영향을 주지 않는다. 이미 응답이 나간 뒤다.
 * 그래서 조용히 넘기지 않고 반드시 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatcher {

    private final PushDelivery pushDelivery;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            pushDelivery.deliver(event);
        } catch (Exception e) {
            log.error("푸시 발송 처리 중 예외. parentId={}", event.parentId(), e);
        }
    }
}
