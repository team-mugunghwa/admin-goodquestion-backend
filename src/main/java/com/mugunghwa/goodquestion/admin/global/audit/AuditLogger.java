package com.mugunghwa.goodquestion.admin.global.audit;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 감사 로그를 남긴다.
 *
 * <p>호출부의 트랜잭션에 그대로 참여한다(별도 전파 설정 없음). 조작이 롤백되면 로그도
 * 같이 사라져야 한다 - 남아 있으면 "삭제했다"는 기록만 있고 데이터는 그대로인 상태가
 * 되어 로그를 믿을 수 없게 된다.
 *
 * <p>IP는 {@code RequestContextHolder}에서 꺼낸다. 서비스 계층까지
 * {@code HttpServletRequest}를 넘기면 모든 메서드 시그니처가 웹에 묶인다.
 */
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogRepository repository;

    public void log(AdminPrincipal admin, AuditAction action, String targetType,
                    UUID targetId, String summary) {
        log(admin, action, targetType, targetId == null ? null : targetId.toString(), summary);
    }

    public void log(AdminPrincipal admin, AuditAction action, String targetType,
                    String targetId, String summary) {
        repository.save(AuditLog.builder()
                .adminId(admin == null ? null : admin.id())
                .adminEmail(admin == null ? "unknown" : admin.email())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .summary(summary)
                .ip(currentIp())
                .build());
    }

    /**
     * 로그인 실패처럼 인증되지 않은 상태에서 남기는 기록.
     * 계정이 존재하지 않을 수도 있으므로 이메일 문자열만 받는다.
     */
    public void logAnonymous(String email, AuditAction action, String targetType, String summary) {
        repository.save(AuditLog.builder()
                .adminEmail(email)
                .action(action)
                .targetType(targetType)
                .summary(summary)
                .ip(currentIp())
                .build());
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return ClientIpResolver.resolve(request);
        }
        // 스케줄러나 테스트처럼 요청 밖에서 부른 경우. 로그를 포기하지는 않는다.
        return null;
    }
}
