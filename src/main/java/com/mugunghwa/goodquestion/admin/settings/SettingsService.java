package com.mugunghwa.goodquestion.admin.settings;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.settings.dto.SettingsDtos.TtsVendorView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본 서비스 런타임 설정 전환 — 첫 용도는 TTS 벤더다.
 *
 * <p>Gemini 무료 등급은 한도가 빠듯해 테스트 중 크레딧이 녹는다. 테스트 기간에는
 * Chirp 3: HD(월 100만 자 무료)로 내렸다가 시연 때 올린다. 본 서버가 합성마다
 * app_settings 행을 읽으므로 <b>저장 즉시, 재배포 없이</b> 적용된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private static final String TARGET_TYPE = "SETTING";
    private static final String TTS_VENDOR_KEY = "tts.vendor";

    private final AppSettingRepository repository;
    private final AuditLogger auditLogger;

    public TtsVendorView ttsVendor() {
        return repository.findById(TTS_VENDOR_KEY)
                .map(setting -> new TtsVendorView(
                        TtsVendor.valueOf(setting.getValue()), setting.getUpdatedAt()))
                // 행이 없으면 본 서버는 환경변수 기본값(운영 기본 OPENAI)으로 돈다
                .orElseGet(() -> new TtsVendorView(TtsVendor.OPENAI, null));
    }

    @Transactional
    public TtsVendorView changeTtsVendor(AdminPrincipal admin, TtsVendor vendor) {
        AppSetting setting = repository.findById(TTS_VENDOR_KEY)
                .map(existing -> {
                    existing.change(vendor.name());
                    return existing;
                })
                .orElseGet(() -> repository.save(new AppSetting(TTS_VENDOR_KEY, vendor.name())));

        // 목소리가 갑자기 바뀌면 원인 추적이 어려우니 누가 언제 전환했는지 남긴다
        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, TTS_VENDOR_KEY,
                "TTS 벤더 전환: %s".formatted(vendor));
        return new TtsVendorView(vendor, setting.getUpdatedAt());
    }
}
