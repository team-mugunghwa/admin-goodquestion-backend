package com.mugunghwa.goodquestion.admin.notice;

/** 공지 분류. 사용자 앱이 목록에서 배지로 보여준다. */
public enum NoticeCategory {
    GENERAL,
    /** 기능 추가·변경 안내 */
    UPDATE,
    EVENT,
    /** 점검 안내. 대개 고정(pinned)과 함께 쓴다. */
    MAINTENANCE
}
