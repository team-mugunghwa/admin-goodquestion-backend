package com.mugunghwa.goodquestion.admin.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, UUID> {

    Optional<AdminRefreshToken> findByTokenHash(String tokenHash);

    List<AdminRefreshToken> findAllByAdminIdAndRevokedAtIsNull(UUID adminId);
}
