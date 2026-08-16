package com.mugunghwa.goodquestion.admin.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 본 서비스의 런타임 설정(app_settings) — 이 콘솔이 쓰고 본 서버가 읽는다.
 *
 * <p>표는 본 서버 마이그레이션(V16)이 만든다. <b>본 서버가 먼저 배포돼야</b>
 * 이 엔티티의 validate가 통과한다(멤버·문의처럼 본 스키마를 매핑하는 다른
 * 모듈과 같은 규칙).
 */
@Entity
@Table(name = "app_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSetting {

    @Id
    @Column(length = 64)
    private String key;

    @Column(nullable = false, length = 128)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
        this.updatedAt = OffsetDateTime.now();
    }

    void change(String value) {
        this.value = value;
        this.updatedAt = OffsetDateTime.now();
    }
}
