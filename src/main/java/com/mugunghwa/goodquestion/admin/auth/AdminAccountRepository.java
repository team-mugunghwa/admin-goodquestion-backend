package com.mugunghwa.goodquestion.admin.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {

    Optional<AdminAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<AdminAccount> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
