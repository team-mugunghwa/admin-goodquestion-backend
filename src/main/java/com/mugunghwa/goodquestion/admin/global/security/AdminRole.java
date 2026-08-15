package com.mugunghwa.goodquestion.admin.global.security;

/**
 * 관리자 권한.
 *
 * <p>둘로만 나눈다. 세분화된 권한 모델(메뉴별 읽기/쓰기)은 운영자가 여럿이고 역할이
 * 갈릴 때 의미가 있는데, 지금은 전원이 모든 메뉴를 본다. 실제로 갈라야 하는 것은
 * "관리자 계정을 만들고 지울 수 있는가" 하나뿐이라 그것만 나눈다.
 */
public enum AdminRole {
    /** 콘텐츠와 고객센터를 다룬다. */
    ADMIN,
    /** ADMIN이 하는 모든 것 + 관리자 계정 관리. */
    SUPER_ADMIN;

    /** 스프링 시큐리티 권한 문자열. {@code hasRole("SUPER_ADMIN")}이 이 값을 본다. */
    public String authority() {
        return "ROLE_" + name();
    }
}
