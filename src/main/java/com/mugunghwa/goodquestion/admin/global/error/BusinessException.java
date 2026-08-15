package com.mugunghwa.goodquestion.admin.global.error;

import lombok.Getter;

/** 업무 규칙에 걸린 요청. {@link GlobalExceptionHandler}가 상태 코드와 본문으로 바꾼다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
