package com.mugunghwa.goodquestion.admin.settings;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.settings.dto.SettingsDtos.TtsVendorChangeRequest;
import com.mugunghwa.goodquestion.admin.settings.dto.SettingsDtos.TtsVendorView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/tts-vendor")
    public TtsVendorView ttsVendor() {
        return settingsService.ttsVendor();
    }

    @PutMapping("/tts-vendor")
    public TtsVendorView changeTtsVendor(@CurrentAdmin AdminPrincipal admin,
                                         @Valid @RequestBody TtsVendorChangeRequest request) {
        return settingsService.changeTtsVendor(admin, request.vendor());
    }
}
