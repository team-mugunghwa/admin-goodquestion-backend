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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 문의 답변. 문의당 한 건이다(DB에 unique).
 *
 * <p>여러 건을 허용하면 사용자 화면이 "어느 것이 최종 답변인가"를 판단해야 하고,
 * 수정과 추가 답변이 구분되지 않는다. 내용을 고치는 것은 같은 행의 update다.
 */
@Entity
@Table(name = "inquiry_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    /** 계정이 지워지면 null이 된다. 누가 답했는지는 adminName에 남는다. */
    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "admin_name", nullable = false, length = 50)
    private String adminName;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public InquiryAnswer(UUID inquiryId, UUID adminId, String adminName, String content) {
        this.inquiryId = inquiryId;
        this.adminId = adminId;
        this.adminName = adminName;
        this.content = content;
    }

    void updateContent(String content, UUID adminId, String adminName) {
        this.content = content;
        // 마지막으로 손댄 사람으로 갱신한다. 사용자가 보는 것은 최종 답변 하나뿐이라
        // 처음 쓴 사람 이름을 남겨 두면 실제 응대한 사람과 어긋난다.
        this.adminId = adminId;
        this.adminName = adminName;
    }
}
