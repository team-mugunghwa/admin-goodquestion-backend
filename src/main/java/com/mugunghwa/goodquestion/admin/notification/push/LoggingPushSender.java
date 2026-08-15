package com.mugunghwa.goodquestion.admin.notification.push;

import lombok.extern.slf4j.Slf4j;

/**
 * 자격증명이 없을 때 쓰는 발송기. 로그만 남기고 성공으로 처리한다.
 *
 * <p>Firebase 키 없이도 앱이 뜨고 테스트가 돌게 하는 것이 목적이다. 알림 자체는
 * DB에 쌓이므로 사용자는 앱 안에서 답변 알림을 볼 수 있다 - 푸시만 나가지 않는다.
 *
 * <p>실패가 아니라 성공으로 돌려주는 이유: 여기서 FAILED를 내면 로컬 개발 중에
 * 답변 등록마다 발송 실패 경고가 쌓여, 정작 운영에서 나는 진짜 실패를 무시하게 된다.
 * "자격증명이 없다"는 사실은 기동 시 한 번 경고로 알린다.
 */
@Slf4j
public class LoggingPushSender implements PushSender {

    public LoggingPushSender() {
        log.warn("FCM 자격증명이 없어 푸시를 실제로 보내지 않습니다. "
                + "알림은 DB에 쌓이므로 앱 안에서는 확인할 수 있습니다. "
                + "발송하려면 FCM_CREDENTIALS를 설정하세요.");
    }

    @Override
    public PushResult send(String deviceToken, PushMessage message) {
        log.info("[푸시 미발송] token={}... title={} link={}",
                deviceToken.length() > 12 ? deviceToken.substring(0, 12) : deviceToken,
                message.title(), message.linkPath());
        return PushResult.SENT;
    }
}
