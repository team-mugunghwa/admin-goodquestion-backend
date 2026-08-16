package com.mugunghwa.goodquestion.admin.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문의 담당자. 문의 하나에 한 명이라 문의 id가 곧 기본키다.
 *
 * <p>두 명이 같은 문의를 동시에 잡고 답하는 사고를 막는 것이 목적이다. 담당자를
 * 바꿀 때는 행을 갈아끼운다.
 */
@Entity
@Table(name = "admin_inquiry_assignees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAssignee {

    @Id
    @Column(name = "inquiry_id")
    private UUID inquiryId;

    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "admin_email", nullable = false, length = 255)
    private String adminEmail;

    @Column(name = "assigned_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime assignedAt;

    @Builder
    public InquiryAssignee(UUID inquiryId, UUID adminId, String adminEmail) {
        this.inquiryId = inquiryId;
        this.adminId = adminId;
        this.adminEmail = adminEmail;
    }
}
