package com.mugunghwa.goodquestion.admin.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {

    List<LoginSession> findAllByParentIdOrderByCreatedAtDesc(UUID parentId);

    List<LoginSession> findAllByParentIdAndRevokedAtIsNull(UUID parentId);
}
