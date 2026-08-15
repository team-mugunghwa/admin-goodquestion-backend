package com.mugunghwa.goodquestion.admin.support;

/** 문의 분류. 사용자가 작성 시 고른다. */
public enum InquiryCategory {
    ACCOUNT,
    PAYMENT,
    /** 이야기 내용, 표현 */
    CONTENT,
    /** 오류 신고 */
    BUG,
    SUGGESTION,
    ETC
}
