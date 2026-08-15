package com.mugunghwa.goodquestion.admin.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 관리자를 컨트롤러 파라미터로 주입한다.
 *
 * <pre>{@code
 * @PostMapping
 * public NoticeResponse create(@CurrentAdmin AdminPrincipal admin, @RequestBody ... ) { ... }
 * }</pre>
 *
 * <p>관리자 식별자를 요청 본문이나 쿼리로 받지 않는다. 받으면 남의 이름으로 조작을
 * 남길 수 있고, 감사 로그가 근거로서 의미를 잃는다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAdmin {
}
