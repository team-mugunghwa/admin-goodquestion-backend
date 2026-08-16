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
 * 자주 쓰는 답변 템플릿.
 *
 * <p>본문의 변수({보호자} 등) 치환은 화면이 한다. 서버는 원문 그대로 보관한다 -
 * 어느 문의에 쓸지는 서버가 모르는 정보다.
 */
@Entity
@Table(name = "admin_reply_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public ReplyTemplate(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public void update(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
