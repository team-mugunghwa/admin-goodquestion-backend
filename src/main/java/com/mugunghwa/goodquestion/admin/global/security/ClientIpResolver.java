package com.mugunghwa.goodquestion.admin.global.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청한 클라이언트의 IP를 구한다. 로그인 기록과 감사 로그에 남긴다.
 *
 * <p>PaaS는 프록시 뒤에 있어 getRemoteAddr()가 프록시 주소를 준다.
 * X-Forwarded-For의 첫 항목이 원래 클라이언트다.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwarded.split(",")[0].trim();
    }
}
