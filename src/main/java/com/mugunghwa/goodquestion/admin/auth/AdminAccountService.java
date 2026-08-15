package com.mugunghwa.goodquestion.admin.auth;

import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.AdminSummary;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.CreateAdminRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.UpdateAdminRequest;
import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 관리자 계정 관리. 최고관리자만 부를 수 있다(컨트롤러의 {@code @PreAuthorize}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAccountService {

    private static final String TARGET_TYPE = "ADMIN_ACCOUNT";

    private final AdminAccountRepository accountRepository;
    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    public PageResponse<AdminSummary> list(int page, int size) {
        return PageResponse.of(
                accountRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100))),
                AdminSummary::from);
    }

    @Transactional
    public AdminSummary create(AdminPrincipal actor, CreateAdminRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        AdminAccount account = accountRepository.save(AdminAccount.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(request.role())
                .build());

        auditLogger.log(actor, AuditAction.CREATE, TARGET_TYPE, account.getId(),
                "관리자 계정 생성: %s (%s)".formatted(account.getEmail(), account.getRole()));
        return AdminSummary.from(account);
    }

    @Transactional
    public AdminSummary update(AdminPrincipal actor, UUID adminId, UpdateAdminRequest request) {
        AdminAccount account = load(adminId);

        // 자기 자신의 권한을 낮추거나 스스로를 정지시키면 마지막 최고관리자가 사라질 수 있다.
        // 그 상태는 DB를 직접 고치는 것 말고는 되돌릴 방법이 없다.
        boolean selfDemotion = account.getId().equals(actor.id())
                && (request.status() != null || request.role() != null);
        if (selfDemotion) {
            throw new BusinessException(ErrorCode.SELF_MODIFICATION_DENIED,
                    "자신의 권한과 상태는 다른 최고관리자만 바꿀 수 있습니다.");
        }

        account.updateProfile(request.name(), request.role());
        if (request.status() != null) {
            account.changeStatus(request.status());
            if (request.status() == AdminStatus.SUSPENDED) {
                // 정지시켰는데 이미 발급된 토큰으로 계속 들어올 수 있으면 정지가 아니다.
                refreshTokenRepository.findAllByAdminIdAndRevokedAtIsNull(account.getId())
                        .forEach(AdminRefreshToken::revoke);
            }
            auditLogger.log(actor,
                    request.status() == AdminStatus.SUSPENDED ? AuditAction.SUSPEND : AuditAction.RESTORE,
                    TARGET_TYPE, account.getId(),
                    "관리자 상태 변경: %s -> %s".formatted(account.getEmail(), request.status()));
        } else {
            auditLogger.log(actor, AuditAction.UPDATE, TARGET_TYPE, account.getId(),
                    "관리자 정보 수정: %s".formatted(account.getEmail()));
        }
        return AdminSummary.from(account);
    }

    @Transactional
    public void delete(AdminPrincipal actor, UUID adminId) {
        AdminAccount account = load(adminId);
        if (account.getId().equals(actor.id())) {
            throw new BusinessException(ErrorCode.SELF_MODIFICATION_DENIED,
                    "자신의 계정은 삭제할 수 없습니다.");
        }
        // 감사 로그의 admin_id는 on delete set null이라 기록 자체는 남는다(admin_email로 식별).
        accountRepository.delete(account);
        auditLogger.log(actor, AuditAction.DELETE, TARGET_TYPE, account.getId(),
                "관리자 계정 삭제: %s".formatted(account.getEmail()));
    }

    private AdminAccount load(UUID adminId) {
        return accountRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 계정을 찾을 수 없습니다."));
    }
}
