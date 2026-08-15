package com.mugunghwa.goodquestion.admin.member;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.security.CurrentAdmin;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.MemberDetail;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.MemberSummary;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.StorySessionResponse;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.SuspendRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 사용자 관리.
 *
 * <p>사용자를 만들거나 지우는 API는 없다. 가입은 사용자가 하고, 탈퇴는 사용자의 권리라
 * 관리자가 대신 누르는 자리를 만들지 않는다. 관리자가 할 수 있는 것은 막는 것(정지)과
 * 끊는 것(로그인 세션 종료)까지다.
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public PageResponse<MemberSummary> list(@RequestParam(required = false) ParentStatus status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return memberService.list(status, keyword, page, size);
    }

    @GetMapping("/{parentId}")
    public MemberDetail get(@PathVariable UUID parentId) {
        return memberService.get(parentId);
    }

    /** 이 보호자의 아이들이 진행한 학습 세션. */
    @GetMapping("/{parentId}/sessions")
    public PageResponse<StorySessionResponse> listSessions(@PathVariable UUID parentId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return memberService.listSessions(parentId, page, size);
    }

    @PostMapping("/{parentId}/suspend")
    public MemberDetail suspend(@CurrentAdmin AdminPrincipal admin,
                                @PathVariable UUID parentId,
                                @Valid @RequestBody SuspendRequest request) {
        return memberService.suspend(admin, parentId, request.reason());
    }

    @PostMapping("/{parentId}/restore")
    public MemberDetail restore(@CurrentAdmin AdminPrincipal admin, @PathVariable UUID parentId) {
        return memberService.restore(admin, parentId);
    }

    /** 로그인 세션을 전부 끊는다. 계정은 그대로 두고 다시 로그인하게 만든다. */
    @PostMapping("/{parentId}/login-sessions/revoke")
    public ResponseEntity<Void> revokeLoginSessions(@CurrentAdmin AdminPrincipal admin,
                                                    @PathVariable UUID parentId) {
        memberService.revokeLoginSessions(admin, parentId);
        return ResponseEntity.noContent().build();
    }
}
