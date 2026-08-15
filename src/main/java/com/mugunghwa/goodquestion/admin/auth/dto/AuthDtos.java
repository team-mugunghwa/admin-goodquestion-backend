package com.mugunghwa.goodquestion.admin.auth.dto;

import com.mugunghwa.goodquestion.admin.auth.AdminAccount;
import com.mugunghwa.goodquestion.admin.auth.AdminStatus;
import com.mugunghwa.goodquestion.admin.global.security.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 인증과 관리자 계정 관리의 요청/응답.
 *
 * <p>레코드 하나에 파일 하나씩 두면 열 개가 넘는데 전부 필드 서너 개짜리다.
 * 한 파일에 모아 두면 계약 전체가 한눈에 들어온다.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    /**
     * @param expiresIn 액세스 토큰의 남은 초. 프론트가 만료 직전에 미리 재발급하는 데 쓴다.
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            AdminSummary admin
    ) {
    }

    public record AdminSummary(
            UUID id,
            String email,
            String name,
            AdminRole role,
            AdminStatus status,
            OffsetDateTime lastLoginAt,
            OffsetDateTime createdAt
    ) {
        public static AdminSummary from(AdminAccount account) {
            return new AdminSummary(account.getId(), account.getEmail(), account.getName(),
                    account.getRole(), account.getStatus(), account.getLastLoginAt(),
                    account.getCreatedAt());
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 10, max = 72, message = "10자 이상 72자 이하로 입력해 주세요.")
            String newPassword
    ) {
    }

    public record CreateAdminRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 10, max = 72, message = "10자 이상 72자 이하로 입력해 주세요.")
            String password,
            @NotBlank @Size(max = 50) String name,
            AdminRole role
    ) {
    }

    public record UpdateAdminRequest(
            @Size(max = 50) String name,
            AdminRole role,
            AdminStatus status
    ) {
    }
}
