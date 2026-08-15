package com.mugunghwa.goodquestion.admin.notification.push;

/**
 * 발송 한 건의 결과.
 *
 * <p>세 가지로 나누는 이유는 대응이 전부 다르기 때문이다. 실패를 하나로 뭉치면
 * 지워야 할 토큰과 잠시 후 다시 보내면 되는 실패를 구분할 수 없다.
 */
public enum PushResult {
    SENT,
    /** 벤더가 "등록되지 않은 토큰"이라고 답한 경우. 앱 삭제·재설치가 대부분이다. 그 토큰을 비활성으로 바꾼다. */
    TOKEN_INVALID,
    /** 벤더 장애, 타임아웃, 쿼터 초과. 토큰은 멀쩡하므로 건드리지 않는다. */
    FAILED
}
