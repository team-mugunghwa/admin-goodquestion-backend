package com.mugunghwa.goodquestion.admin.guide;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.GuideResponse;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.ReorderRequest;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.UpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    /** 순서를 보며 편집하는 화면이라 페이징하지 않는다. */
    @GetMapping
    public List<GuideResponse> list(@RequestParam(required = false) GuideCategory category,
                                    @RequestParam(required = false) ContentStatus status) {
        return guideService.list(category, status);
    }

    @GetMapping("/{guideId}")
    public GuideResponse get(@PathVariable UUID guideId) {
        return guideService.get(guideId);
    }

    @PostMapping
    public GuideResponse create(@CurrentAdmin AdminPrincipal admin,
                                @Valid @RequestBody CreateRequest request) {
        return guideService.create(admin, request);
    }

    @PatchMapping("/{guideId}")
    public GuideResponse update(@CurrentAdmin AdminPrincipal admin,
                                @PathVariable UUID guideId,
                                @Valid @RequestBody UpdateRequest request) {
        return guideService.update(admin, guideId, request);
    }

    /** 정렬 결과 전체를 보낸다. 배열의 위치가 곧 순서다. */
    @PutMapping("/order")
    public List<GuideResponse> reorder(@CurrentAdmin AdminPrincipal admin,
                                       @Valid @RequestBody ReorderRequest request) {
        return guideService.reorder(admin, request);
    }

    @DeleteMapping("/{guideId}")
    public ResponseEntity<Void> delete(@CurrentAdmin AdminPrincipal admin,
                                       @PathVariable UUID guideId) {
        guideService.delete(admin, guideId);
        return ResponseEntity.noContent().build();
    }
}
