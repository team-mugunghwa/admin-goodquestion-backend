package com.mugunghwa.goodquestion.admin.story;

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

/** 주제 마스터. 사용자 앱의 이야기 목록 필터가 이 순서대로 칩을 그린다. */
@Entity
@Table(name = "topics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Topic(String name, short displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public void update(String name, Short displayOrder) {
        if (name != null) this.name = name;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }
}
