package com.mugunghwa.goodquestion.admin.global.audit;

/**
 * 감사 로그에 남기는 조작 종류.
 *
 * <p>상태를 바꾸는 것만 남긴다. 조회까지 남기면 목록을 한 번 여는 것만으로 수십 건이
 * 쌓여 정작 확인해야 할 삭제와 정지가 묻힌다.
 */
public enum AuditAction {
    LOGIN,
    LOGIN_FAILED,
    CREATE,
    UPDATE,
    DELETE,
    /** 공개/비공개 전환처럼 노출 상태만 바꾼 경우. CREATE/UPDATE와 구분해야 "언제 내렸는가"를 찾을 수 있다. */
    PUBLISH,
    /** 사용자 정지/해제, 로그인 세션 강제 종료 */
    SUSPEND,
    RESTORE,
    REVOKE_SESSION,
    /** 문의 답변 등록/수정 */
    ANSWER,
    /** 사용자에게 알림/푸시를 보낸 경우 */
    NOTIFY
}
