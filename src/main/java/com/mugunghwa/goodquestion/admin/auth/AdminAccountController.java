package com.mugunghwa.goodquestion.admin.auth;

import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.AdminSummary;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.CreateAdminRequest;
import com.mugunghwa.goodquestion.admin.auth.dto.AuthDtos.UpdateAdminRequest;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 관리자 계정 관리. 최고관리자 전용이다. */
@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAccountController {

    private final AdminAccountService service;

    @GetMapping
    public PageResponse<AdminSummary> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @PostMapping
    public AdminSummary create(@CurrentAdmin AdminPrincipal admin,
                               @Valid @RequestBody CreateAdminRequest request) {
        return service.create(admin, request);
    }

    @PatchMapping("/{adminId}")
    public AdminSummary update(@CurrentAdmin AdminPrincipal admin,
                               @PathVariable UUID adminId,
                               @Valid @RequestBody UpdateAdminRequest request) {
        return service.update(admin, adminId, request);
    }

    @DeleteMapping("/{adminId}")
    public ResponseEntity<Void> delete(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID adminId) {
        service.delete(admin, adminId);
        return ResponseEntity.noContent().build();
    }
}
