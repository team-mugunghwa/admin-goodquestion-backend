package com.mugunghwa.goodquestion.admin.notification.push;

/**
 * 푸시 발송기.
 *
 * <p>구현이 둘이다 - 자격증명이 있으면 {@link FcmPushSender}, 없으면
 * {@link LoggingPushSender}. 어느 쪽이 뜨는지는 {@link PushConfig}가 정한다.
 *
 * <p><b>왜 FCM인가.</b> 후보는 FCM, OneSignal, Expo 세 가지였다. Expo는 Expo로 만든
 * React Native 앱 전용이라 Flutter인 이 서비스에는 쓸 수 없다. OneSignal은 콘솔이
 * 편하지만 무료 구간에 사용자 수 제한이 있고, 결국 안드로이드 전달은 내부적으로 FCM을
 * 거치므로 의존 대상이 하나 더 늘어나는 셈이다. FCM은 발송량 무제한 무료이고,
 * Flutter 공식 플러그인(firebase_messaging)이 iOS/안드로이드/웹을 모두 덮는다.
 * 서버 쪽도 REST 한 번이면 되므로 FCM으로 정했다.
 *
 * <p>그래도 인터페이스를 둔 것은 벤더를 바꿀 가능성 때문이 아니라, 자격증명 없이도
 * 앱이 뜨고 테스트가 돌아야 하기 때문이다. 로컬과 CI에 Firebase 키를 두지 않는다.
 */
public interface PushSender {

    /**
     * 기기 하나에 보낸다.
     *
     * <p>예외를 던지지 않는다. 알림 저장은 이미 끝난 뒤에 부르는 것이라, 발송 실패로
     * 예외가 올라가면 "알림은 남았는데 처리가 실패했다"는 애매한 상태가 된다.
     * 실패는 반환값으로 알린다.
     */
    PushResult send(String deviceToken, PushMessage message);
}
