package com.mugunghwa.goodquestion.admin.global.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 관리자 조작 기록. 한 번 쌓이면 수정하지 않는다. */
@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 계정이 지워지면 null이 된다. 그래도 누구였는지는 adminEmail에 남는다. */
    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "admin_email", nullable = false, length = 255)
    private String adminEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    /** 무엇에 대한 조작인가. "NOTICE", "PARENT", "STORY" 같은 리소스 이름. */
    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    /** 사람이 읽을 한 줄. 목록에서 이 문장만 보고 무슨 일이 있었는지 알 수 있어야 한다. */
    @Column(columnDefinition = "text")
    private String summary;

    @Column(length = 45)
    private String ip;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public AuditLog(UUID adminId, String adminEmail, AuditAction action,
                    String targetType, String targetId, String summary, String ip) {
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.summary = summary;
        this.ip = ip;
    }
}
