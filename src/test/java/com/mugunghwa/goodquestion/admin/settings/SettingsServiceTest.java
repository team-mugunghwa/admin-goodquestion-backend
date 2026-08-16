package com.mugunghwa.goodquestion.admin.settings;

import com.mugunghwa.goodquestion.admin.auth.AdminAccount;
import com.mugunghwa.goodquestion.admin.auth.AdminAccountRepository;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogRepository;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.AdminRole;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TTS 벤더 전환 — 본 서버가 합성마다 읽는 app_settings(tts.vendor)를 쓴다.
 *
 * <p>전환은 재배포 없이 즉시 적용되는 운영 행위라, 누가 언제 바꿨는지 감사 로그가
 * 반드시 남아야 한다 — 목소리가 갑자기 바뀌었을 때 추적할 유일한 근거다.
 */
@IntegrationTest
@Transactional
class SettingsServiceTest {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private AppSettingRepository repository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AdminAccountRepository accountRepository;

    // 감사 로그가 admin_accounts를 FK로 물고 있어 실제 계정이 있어야 한다
    private AdminPrincipal admin;

    @BeforeEach
    void setUp() {
        AdminAccount account = accountRepository.save(AdminAccount.builder()
                .email("settings-test-%s@goodquestion.kr".formatted(System.nanoTime()))
                .passwordHash("not-used")
                .name("테스트관리자")
                .role(AdminRole.ADMIN)
                .build());
        admin = new AdminPrincipal(account.getId(), account.getEmail(),
                account.getName(), account.getRole());
    }

    private AdminPrincipal admin() {
        return admin;
    }

    @Test
    void 행이_없으면_본_서버_기본값인_OPENAI로_보여준다() {
        assertThat(settingsService.ttsVendor().vendor()).isEqualTo(TtsVendor.OPENAI);
        assertThat(settingsService.ttsVendor().updatedAt()).isNull();
    }

    @Test
    void 전환하면_행이_생기고_조회에_바로_반영된다() {
        settingsService.changeTtsVendor(admin(), TtsVendor.CHIRP3);

        assertThat(settingsService.ttsVendor().vendor()).isEqualTo(TtsVendor.CHIRP3);
        assertThat(repository.findById("tts.vendor").orElseThrow().getValue())
                .isEqualTo("CHIRP3");
    }

    @Test
    void 다시_전환하면_행_하나를_덮어쓴다() {
        settingsService.changeTtsVendor(admin(), TtsVendor.CHIRP3);
        settingsService.changeTtsVendor(admin(), TtsVendor.GEMINI);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(settingsService.ttsVendor().vendor()).isEqualTo(TtsVendor.GEMINI);
    }

    @Test
    void 전환은_감사_로그를_남긴다() {
        long before = auditLogRepository.count();

        settingsService.changeTtsVendor(admin(), TtsVendor.CHIRP3);

        assertThat(auditLogRepository.count()).isEqualTo(before + 1);
    }
}
