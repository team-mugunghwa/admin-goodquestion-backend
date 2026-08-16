package com.mugunghwa.goodquestion.admin.settings.dto;

import com.mugunghwa.goodquestion.admin.settings.TtsVendor;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public final class SettingsDtos {

    private SettingsDtos() {
    }

    /**
     * @param vendor    지금 적용 중인 벤더. DB 행이 없으면 본 서버 기본값(OPENAI)으로 표시
     * @param updatedAt 마지막 전환 시각. 행이 없으면 null
     */
    public record TtsVendorView(TtsVendor vendor, OffsetDateTime updatedAt) {
    }

    public record TtsVendorChangeRequest(@NotNull TtsVendor vendor) {
    }
}
