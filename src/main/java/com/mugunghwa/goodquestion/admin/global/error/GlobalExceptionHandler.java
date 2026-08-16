package com.mugunghwa.goodquestion.admin.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode(), e.getMessage()));
    }

    /** 어느 필드가 왜 틀렸는지까지 내려준다. 관리자 폼은 입력 항목이 많아 "잘못된 요청"만으로는 못 고친다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.getDefaultMessage());
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    /** 경로/쿼리 파라미터의 타입이 안 맞는 경우(UUID 자리에 문자열 등). 클라이언트 버그라 400이다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, e.getName() + " 값이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다."));
    }

    /**
     * 유니크 제약이나 외래키에 걸린 경우. 여기까지 왔다는 것은 서비스에서 미리 확인하지
     * 못한 경합이라는 뜻이므로 500이 아니라 409로 내리고 로그를 남긴다 - 관리자는
     * 다시 시도하면 되고, 우리는 어떤 제약이 걸렸는지 확인할 수 있어야 한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(DataIntegrityViolationException e) {
        log.warn("제약 위반", e);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONSTRAINT_VIOLATION", "다른 데이터와 충돌해 처리하지 못했습니다."));
    }

    /**
     * 없는 경로로 들어온 요청.
     *
     * <p>이 핸들러가 없으면 아래 {@code Exception} 핸들러가 받아서 500 으로 내린다.
     * 주소를 잘못 친 것뿐인데 서버가 고장 난 것처럼 보이고, 로그에는 스택트레이스가
     * 남아 진짜 오류와 섞인다. 실제로 배포를 확인하다가 이것 때문에 없는 장애를
     * 한참 쫓았다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        log.debug("없는 경로: {}", e.getResourcePath());
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND, "없는 경로입니다: " + e.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
