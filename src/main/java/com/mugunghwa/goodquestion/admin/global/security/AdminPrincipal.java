package com.mugunghwa.goodquestion.admin.global.security;

import java.util.UUID;

/**
 * 인증된 관리자. 토큰에서 꺼낸 값만 담는다.
 *
 * <p>이메일과 이름까지 토큰에 싣는 이유는 감사 로그 때문이다. 조작 한 건마다 관리자
 * 행을 다시 읽으면 모든 쓰기 요청에 조회가 하나씩 붙고, 계정이 나중에 지워지면
 * 로그에서 누구였는지 알 수 없게 된다.
 */
public record AdminPrincipal(UUID id, String email, String name, AdminRole role) {

    public boolean isSuperAdmin() {
        return role == AdminRole.SUPER_ADMIN;
    }
}
