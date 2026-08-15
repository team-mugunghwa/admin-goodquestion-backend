package com.mugunghwa.goodquestion.admin.member.dto;

import com.mugunghwa.goodquestion.admin.member.Child;
import com.mugunghwa.goodquestion.admin.member.LoginSession;
import com.mugunghwa.goodquestion.admin.member.ParentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MemberDtos {

    private MemberDtos() {
    }

    /**
     * 목록 한 줄.
     *
     * @param childCount 등록한 아이 수. 0이면 가입만 하고 쓰지 않는 계정이다.
     */
    public record MemberSummary(
            UUID id,
            String name,
            String email,
            String provider,
            ParentStatus status,
            boolean locked,
            int childCount,
            OffsetDateTime createdAt
    ) {
    }

    public record ChildResponse(UUID id, String name, short birthYear, OffsetDateTime createdAt) {
        public static ChildResponse from(Child child) {
            return new ChildResponse(child.getId(), child.getName(), child.getBirthYear(),
                    child.getCreatedAt());
        }
    }

    public record LoginSessionResponse(
            UUID id,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt
    ) {
        public static LoginSessionResponse from(LoginSession session) {
            return new LoginSessionResponse(session.getId(), session.isActive(),
                    session.getCreatedAt(), session.getExpiresAt(), session.getRevokedAt());
        }
    }

    /**
     * 학습 세션 한 줄.
     *
     * @param safetyFlagged 아이 발화에서 위험 신호가 감지된 세션. 확인이 필요하다.
     */
    public record StorySessionResponse(
            UUID id,
            UUID childId,
            String childName,
            UUID storyId,
            String storyTitle,
            String status,
            boolean safetyFlagged,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            OffsetDateTime lastActivityAt
    ) {
    }

    public record MemberDetail(
            UUID id,
            String name,
            String email,
            String provider,
            ParentStatus status,
            boolean locked,
            OffsetDateTime lockedUntil,
            String suspendedReason,
            OffsetDateTime suspendedAt,
            String lastLoginIp,
            OffsetDateTime createdAt,
            List<ChildResponse> children,
            List<LoginSessionResponse> loginSessions,
            long inquiryCount
    ) {
    }

    public record SuspendRequest(
            @NotBlank @Size(max = 500, message = "500자 이하로 입력해 주세요.")
            String reason
    ) {
    }
}
