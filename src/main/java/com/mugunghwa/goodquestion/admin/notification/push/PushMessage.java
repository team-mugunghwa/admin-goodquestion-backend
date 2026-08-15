package com.mugunghwa.goodquestion.admin.notification.push;

import java.util.Map;

/**
 * 발송기에 넘기는 알림 한 건.
 *
 * @param linkPath 앱이 알림을 눌렀을 때 이동할 화면 경로. 벤더의 data 페이로드로 실린다.
 */
public record PushMessage(String title, String body, String linkPath, Map<String, String> data) {

    public static PushMessage of(String title, String body, String linkPath) {
        return new PushMessage(title, body, linkPath, Map.of());
    }
}
