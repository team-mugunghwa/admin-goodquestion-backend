package com.mugunghwa.goodquestion.admin.support;

public enum InquiryStatus {
    /** 답변 대기. 관리자 목록의 기본 화면이 이것만 오래된 순으로 보여준다. */
    PENDING,
    ANSWERED,
    /** 종료. 답변 없이 닫은 것도 포함한다(중복 문의 등). */
    CLOSED
}
