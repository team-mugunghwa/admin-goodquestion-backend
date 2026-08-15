package com.mugunghwa.goodquestion.admin.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 관리자 API가 내려주는 오류 코드.
 *
 * <p>프론트는 HTTP 상태나 메시지 문자열이 아니라 이 이름으로 분기한다. 서비스
 * 백엔드의 ErrorCode와 이름이 겹치는 것들이 있지만 값을 공유하지 않는다 - 두
 * 애플리케이션이 각자 배포되므로 한쪽 변경이 다른 쪽을 조용히 깨뜨리면 안 된다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // ---- 관리자 인증 ----
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 많아 계정이 잠겼습니다. 잠시 후 다시 시도해 주세요."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다. 최고관리자에게 문의해 주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    /** 자기 계정을 스스로 정지하거나 지우는 것을 막는다. 관리자가 전부 사라지는 사고를 막는 최소한의 장치다. */
    SELF_MODIFICATION_DENIED(HttpStatus.CONFLICT, "자신의 계정에는 할 수 없는 조작입니다."),

    // ---- 콘텐츠 ----
    /** 세션이 참조 중인 이야기는 지우지 않는다. 지우면 진행 기록과 리포트가 끊긴다. */
    STORY_IN_USE(HttpStatus.CONFLICT, "이미 진행된 기록이 있는 이야기는 삭제할 수 없습니다. 보관 처리해 주세요."),
    DUPLICATE_SCENE_ORDER(HttpStatus.CONFLICT, "같은 순서의 장면이 이미 있습니다."),
    /** 대화 장면은 캐릭터/목표/턴 수가 갖춰져야 서비스가 그 장면을 실행할 수 있다. */
    INCOMPLETE_DIALOGUE_SCENE(HttpStatus.UNPROCESSABLE_ENTITY, "대화 장면은 캐릭터, 첫 대사, 장면 목표, 턴 수가 모두 필요합니다."),
    DUPLICATE_TOPIC(HttpStatus.CONFLICT, "이미 있는 주제입니다."),

    // ---- 고객센터 ----
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 답변이 등록된 문의입니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 답변이 없습니다."),
    INQUIRY_CLOSED(HttpStatus.CONFLICT, "종료된 문의에는 답변할 수 없습니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
