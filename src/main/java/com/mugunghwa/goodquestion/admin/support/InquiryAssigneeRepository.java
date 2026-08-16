package com.mugunghwa.goodquestion.admin.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InquiryAssigneeRepository extends JpaRepository<InquiryAssignee, UUID> {
}
