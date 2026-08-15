package com.mugunghwa.goodquestion.admin.auth;

import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.AdminSummary;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.ChangePasswordRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.LoginRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.RefreshRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.TokenResponse;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.ClientIpResolver;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest servletRequest) {
        return authService.login(request, ClientIpResolver.resolve(servletRequest));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /** 본문은 선택이다. 토큰을 실어 보내면 그 기기만, 비우면 이 계정의 모든 기기가 로그아웃된다. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentAdmin AdminPrincipal admin,
                                       @RequestBody(required = false) RefreshRequest request) {
        authService.logout(admin, request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AdminSummary me(@CurrentAdmin AdminPrincipal admin) {
        return authService.me(admin);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@CurrentAdmin AdminPrincipal admin,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(admin, request);
        return ResponseEntity.noContent().build();
    }
}
