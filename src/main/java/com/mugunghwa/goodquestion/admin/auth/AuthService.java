package com.mugunghwa.goodquestion.admin.auth;

import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.AdminSummary;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.ChangePasswordRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.LoginRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.TokenResponse;
import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminJwtProvider;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TARGET_TYPE = "ADMIN_ACCOUNT";

    private final AdminAccountRepository accountRepository;
    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final AdminJwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @Value("${admin.security.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${admin.security.lock-duration}")
    private Duration lockDuration;

    @Value("${admin.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public TokenResponse login(LoginRequest request, String ip) {
        AdminAccount account = accountRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    // 존재하지 않는 계정으로의 시도도 남긴다. 어떤 이메일을 찔러보고 있는지가
                    // 무차별 대입을 알아채는 첫 신호다.
                    auditLogger.logAnonymous(request.email(), AuditAction.LOGIN_FAILED,
                            TARGET_TYPE, "없는 계정으로 로그인 시도");
                    // 있는 계정인지 없는 계정인지 응답으로 구분되지 않게 같은 오류를 쓴다.
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (account.isSuspended()) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (account.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            account.recordLoginFailure(maxLoginAttempts, lockDuration);
            auditLogger.logAnonymous(account.getEmail(), AuditAction.LOGIN_FAILED, TARGET_TYPE,
                    "비밀번호 불일치 (%d회 연속)".formatted(account.getFailedLoginAttempts()));
            // 잠금에 막 걸린 요청은 "잠겼다"로 알려 준다. 그렇지 않으면 관리자가
            // 계속 같은 오류를 보며 비밀번호만 의심한다.
            throw new BusinessException(
                    account.isLocked() ? ErrorCode.ACCOUNT_LOCKED : ErrorCode.INVALID_CREDENTIALS);
        }

        account.recordLoginSuccess(ip);
        auditLogger.log(account.toPrincipal(), AuditAction.LOGIN, TARGET_TYPE,
                account.getId(), "로그인");
        return issueTokens(account);
    }

    /**
     * 리프레시 토큰으로 액세스 토큰을 다시 발급한다.
     *
     * <p>쓴 토큰은 즉시 폐기하고 새 토큰을 준다(회전). 같은 토큰이 두 번 오면 두 번째는
     * 거절되므로, 탈취가 있었다면 정상 사용자 쪽이 재로그인을 하게 되어 드러난다.
     */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        AdminRefreshToken token = refreshTokenRepository
                .findByTokenHash(AdminTokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!token.isUsable()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        AdminAccount account = accountRepository.findById(token.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (account.isSuspended()) {
            // 정지된 계정의 토큰은 남아 있어도 더 쓰이면 안 된다.
            token.revoke();
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }

        token.revoke();
        return issueTokens(account);
    }

    @Transactional
    public void logout(AdminPrincipal admin, String rawRefreshToken) {
        // 리프레시 토큰을 같이 보내면 그것만, 없으면 그 계정의 것을 전부 폐기한다.
        // 관리자가 여러 기기에서 쓰는 경우가 드물어 "전부 로그아웃"이 기대에 가깝다.
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(AdminTokenHasher.hash(rawRefreshToken))
                    .ifPresent(AdminRefreshToken::revoke);
            return;
        }
        refreshTokenRepository.findAllByAdminIdAndRevokedAtIsNull(admin.id())
                .forEach(AdminRefreshToken::revoke);
    }

    public AdminSummary me(AdminPrincipal admin) {
        return AdminSummary.from(loadAccount(admin.id()));
    }

    @Transactional
    public void changePassword(AdminPrincipal admin, ChangePasswordRequest request) {
        AdminAccount account = loadAccount(admin.id());
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호가 올바르지 않습니다.");
        }
        account.changePassword(passwordEncoder.encode(request.newPassword()));

        // 비밀번호를 바꾼 이유가 유출 의심일 수 있다. 다른 기기의 세션을 남겨 두면
        // 바꾼 의미가 없어지므로 발급된 리프레시 토큰을 모두 끊는다.
        refreshTokenRepository.findAllByAdminIdAndRevokedAtIsNull(account.getId())
                .forEach(AdminRefreshToken::revoke);

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, account.getId(), "비밀번호 변경");
    }

    AdminAccount loadAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관리자 계정을 찾을 수 없습니다."));
    }

    private TokenResponse issueTokens(AdminAccount account) {
        String rawRefreshToken = generateRefreshToken();
        refreshTokenRepository.save(AdminRefreshToken.builder()
                .adminId(account.getId())
                .tokenHash(AdminTokenHasher.hash(rawRefreshToken))
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .build());

        return new TokenResponse(
                jwtProvider.issue(account.toPrincipal()),
                rawRefreshToken,
                jwtProvider.getExpiresInSeconds(),
                AdminSummary.from(account));
    }

    /** 256비트 난수. 토큰 자체에 의미를 담지 않으므로 DB 조회로만 검증된다. */
    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
