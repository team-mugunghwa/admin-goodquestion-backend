package com.mugunghwa.goodquestion.admin.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용자 알림 발행.
 *
 * <p>알림 저장은 부르는 쪽 트랜잭션 안에서 끝난다. 푸시는 그 트랜잭션이 커밋된 뒤에
 * 나간다({@link PushDispatcher}). 순서가 반대면 커밋이 실패했는데 푸시는 이미 나간
 * 상태가 생기고, 사용자는 알림을 받았는데 열어 보면 아무것도 없다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 알림을 저장하고 커밋 후 푸시를 예약한다.
     *
     * @param linkPath 앱이 알림을 눌렀을 때 이동할 경로. 없으면 알림함에 머문다.
     */
    @Transactional
    public Notification notify(UUID parentId, NotificationType type,
                               String title, String body, String linkPath) {
        Notification notification = notificationRepository.save(Notification.builder()
                .parentId(parentId)
                .type(type)
                .title(title)
                .body(body)
                .linkPath(linkPath)
                .build());

        eventPublisher.publishEvent(new NotificationCreatedEvent(parentId, title, body, linkPath));
        return notification;
    }

    /** 저장된 알림을 푸시로 알리라는 신호. 커밋 후에 처리된다. */
    public record NotificationCreatedEvent(UUID parentId, String title, String body, String linkPath) {
    }
}
