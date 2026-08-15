package com.mugunghwa.goodquestion.admin.member;

public enum ParentStatus {
    ACTIVE,
    /** 관리자가 막은 계정. 로그인이 거부된다. 데이터는 그대로 남는다. */
    SUSPENDED
}
