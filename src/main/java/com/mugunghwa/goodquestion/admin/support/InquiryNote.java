package com.mugunghwa.goodquestion.admin.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문의 내부 메모. 사용자에게 보이지 않는 팀 안의 기록이다.
 *
 * <p>수정과 삭제가 없다. "전화드리기로 했다" 같은 처리 맥락의 기록이라, 고칠 수
 * 있게 두면 나중에 "그때 뭐라고 적혀 있었나"를 믿을 수 없게 된다.
 */
@Entity
@Table(name = "admin_inquiry_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    /** 계정이 지워지면 null이 된다. 누구였는지는 authorEmail에 남는다. */
    @Column(name = "author_admin_id")
    private UUID authorAdminId;

    @Column(name = "author_email", nullable = false, length = 255)
    private String authorEmail;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public InquiryNote(UUID inquiryId, UUID authorAdminId, String authorEmail, String body) {
        this.inquiryId = inquiryId;
        this.authorAdminId = authorAdminId;
        this.authorEmail = authorEmail;
        this.body = body;
    }
}
