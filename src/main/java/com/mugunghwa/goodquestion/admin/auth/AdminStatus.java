package com.mugunghwa.goodquestion.admin.auth;

public enum AdminStatus {
    ACTIVE,
    /** 퇴사/역할 변경 등으로 접근을 막은 계정. 지우지 않는 이유는 감사 로그가 이 계정을 가리키기 때문이다. */
    SUSPENDED
}
