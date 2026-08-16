package com.mugunghwa.goodquestion.admin.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReplyTemplateRepository extends JpaRepository<ReplyTemplate, UUID> {

    /** 최근에 손댄 것이 위로. 자주 쓰는 템플릿이 자연스럽게 앞에 온다. */
    List<ReplyTemplate> findAllByOrderByUpdatedAtDesc();
}
