package com.mugunghwa.goodquestion.admin.auth;

import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.LoginRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.TokenResponse;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminJwtProvider;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.AdminRole;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class AuthServiceTest {

    private static final String PASSWORD = "test-password-1234";

    @Autowired AuthService authService;
    @Autowired AdminAccountRepository accountRepository;
    @Autowired AdminRefreshTokenRepository refreshTokenRepository;
    @Autowired AdminJwtProvider jwtProvider;
    @Autowired PasswordEncoder passwordEncoder;

    private AdminAccount account;

    @BeforeEach
    void setUp() {
        account = accountRepository.save(AdminAccount.builder()
                .email("auth-test-%s@goodquestion.kr".formatted(System.nanoTime()))
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("테스트관리자")
                .role(AdminRole.ADMIN)
                .build());
    }

    @Test
    @DisplayName("로그인에 성공하면 액세스 토큰에 권한이 담기고 리프레시 토큰이 저장된다")
    void loginIssuesTokens() {
        TokenResponse response = authService.login(
                new LoginRequest(account.getEmail(), PASSWORD), "127.0.0.1");

        AdminPrincipal principal = jwtProvider.verify(response.accessToken());
        assertThat(principal.id()).isEqualTo(account.getId());
        assertThat(principal.role()).isEqualTo(AdminRole.ADMIN);
        assertThat(refreshTokenRepository.findAllByAdminIdAndRevokedAtIsNull(account.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("비밀번호를 틀리면 실패 횟수가 쌓이고 상한을 넘는 순간 잠긴다")
    void lockAfterRepeatedFailures() {
        // application.yml의 기본 상한은 5회다.
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login(
                    new LoginRequest(account.getEmail(), "wrong-password"), "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        // 5회째는 잠금에 걸리므로 오류가 바뀐다. 그냥 INVALID_CREDENTIALS로 두면
        // 관리자가 비밀번호만 계속 의심하게 된다.
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(account.getEmail(), "wrong-password"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        // 잠긴 뒤에는 올바른 비밀번호도 통과하지 못한다.
        assertThatThrownBy(() -> authService.login(
                new LoginRequest(account.getEmail(), PASSWORD), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("리프레시 토큰은 한 번 쓰면 폐기되고 새 토큰으로 교체된다")
    void refreshRotatesToken() {
        TokenResponse first = authService.login(
                new LoginRequest(account.getEmail(), PASSWORD), "127.0.0.1");

        TokenResponse second = authService.refresh(first.refreshToken());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

        // 같은 토큰이 두 번 오면 그중 하나는 탈취된 것이다. 회전을 두면 그 상황이 거절로 드러난다.
        assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("없는 계정과 비밀번호 불일치는 같은 오류를 낸다")
    void doesNotRevealAccountExistence() {
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("nobody@goodquestion.kr", PASSWORD), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
