package com.mugunghwa.goodquestion.admin.support;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 고객센터 문의. 사용자 앱이 만들고 관리자가 답변한다.
 *
 * <p>관리자 콘솔에는 생성 API가 없다. 문의는 사용자만 만든다 - 관리자가 대신 만들 수
 * 있으면 그 문의의 작성자가 누구인지가 흐려지고, 답변 알림이 엉뚱한 사람에게 간다.
 */
@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    /** 답변이 처음 등록된 시점. 이미 답변 상태면 시각을 새로 찍지 않는다(수정과 최초 답변은 다르다). */
    void markAnswered() {
        this.status = InquiryStatus.ANSWERED;
        if (this.answeredAt == null) {
            this.answeredAt = OffsetDateTime.now();
        }
    }

    /**
     * 더 볼 것이 없는 문의를 닫는다.
     *
     * <p>답변하지 않은 문의도 닫을 수 있다. 중복 문의나 문의가 아닌 글이 실제로 들어오고,
     * 그것들에 억지로 답변을 달게 하면 미답변 건수가 실제 남은 일을 나타내지 않게 된다.
     */
    void close() {
        this.status = InquiryStatus.CLOSED;
    }

    void reopen() {
        this.status = answeredAt != null ? InquiryStatus.ANSWERED : InquiryStatus.PENDING;
    }

    public boolean isClosed() {
        return status == InquiryStatus.CLOSED;
    }
}
