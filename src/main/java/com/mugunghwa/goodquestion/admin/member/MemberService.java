package com.mugunghwa.goodquestion.admin.member;

import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.global.web.PageResponse;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.ChildResponse;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.LoginSessionResponse;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.MemberDetail;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.MemberSummary;
import com.mugunghwa.goodquestion.admin.member.dto.MemberDtos.StorySessionResponse;
import com.mugunghwa.goodquestion.admin.story.Story;
import com.mugunghwa.goodquestion.admin.story.StoryRepository;
import com.mugunghwa.goodquestion.admin.support.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 사용자 관리.
 *
 * <p>여기서 다루는 것은 남의 개인정보다. 조회는 감사 로그에 남기지 않지만(목록 한 번에
 * 수십 건이 쌓여 정작 볼 것이 묻힌다) 정지와 로그인 세션 종료는 반드시 남긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final String TARGET_TYPE = "PARENT";

    private final ParentRepository parentRepository;
    private final ChildRepository childRepository;
    private final StorySessionRepository storySessionRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final StoryRepository storyRepository;
    private final InquiryRepository inquiryRepository;
    private final AuditLogger auditLogger;

    public PageResponse<MemberSummary> list(ParentStatus status, String keyword, int page, int size) {
        Page<Parent> parents = parentRepository.search(status,
                StringUtils.hasText(keyword) ? keyword.trim() : "",
                PageRequest.of(page, Math.min(size, 100)));

        // 보호자마다 아이 수를 따로 세면 목록 한 번에 20번의 쿼리가 더 나간다.
        List<UUID> parentIds = parents.getContent().stream().map(Parent::getId).toList();
        Map<UUID, Long> childCounts = parentIds.isEmpty() ? Map.of()
                : childRepository.findAllByParentIdIn(parentIds).stream()
                .collect(Collectors.groupingBy(Child::getParentId, Collectors.counting()));

        return PageResponse.of(parents, parent -> new MemberSummary(
                parent.getId(), parent.getName(), parent.getEmail(), parent.getProvider(),
                parent.getStatus(), parent.isLocked(),
                childCounts.getOrDefault(parent.getId(), 0L).intValue(),
                parent.getCreatedAt()));
    }

    public MemberDetail get(UUID parentId) {
        Parent parent = load(parentId);
        List<ChildResponse> children = childRepository.findAllByParentIdOrderByCreatedAtAsc(parentId)
                .stream().map(ChildResponse::from).toList();
        List<LoginSessionResponse> sessions =
                loginSessionRepository.findAllByParentIdOrderByCreatedAtDesc(parentId)
                        .stream().map(LoginSessionResponse::from).toList();

        return new MemberDetail(parent.getId(), parent.getName(), parent.getEmail(),
                parent.getProvider(), parent.getStatus(), parent.isLocked(),
                parent.getLockedUntil(), parent.getSuspendedReason(), parent.getSuspendedAt(),
                parent.getLastLoginIp(), parent.getCreatedAt(), children, sessions,
                inquiryRepository.findAllByParentIdOrderByCreatedAtDesc(parentId,
                        PageRequest.of(0, 1)).getTotalElements());
    }

    /** 이 보호자의 아이들이 진행한 학습 세션. */
    public PageResponse<StorySessionResponse> listSessions(UUID parentId, int page, int size) {
        List<Child> children = childRepository.findAllByParentIdOrderByCreatedAtAsc(parentId);
        if (children.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        Map<UUID, String> childNames = children.stream()
                .collect(Collectors.toMap(Child::getId, Child::getName));

        Page<StorySession> sessions = storySessionRepository
                .findAllByChildIdInOrderByLastActivityAtDesc(List.copyOf(childNames.keySet()),
                        PageRequest.of(page, Math.min(size, 100)));

        Map<UUID, Story> stories = loadStories(sessions.getContent());

        return PageResponse.of(sessions, session -> new StorySessionResponse(
                session.getId(), session.getChildId(), childNames.get(session.getChildId()),
                session.getStoryId(),
                stories.containsKey(session.getStoryId())
                        ? stories.get(session.getStoryId()).getTitle() : "(삭제된 이야기)",
                session.getStatus(), session.isSafetyFlagged(),
                session.getStartedAt(), session.getCompletedAt(), session.getLastActivityAt()));
    }

    @Transactional
    public MemberDetail suspend(AdminPrincipal admin, UUID parentId, String reason) {
        Parent parent = load(parentId);
        parent.suspend(reason);

        // 정지시켰는데 이미 로그인된 기기가 그대로 쓸 수 있으면 정지가 아니다.
        // 액세스 토큰은 30분까지 남지만 그 뒤로는 재발급이 막힌다.
        loginSessionRepository.findAllByParentIdAndRevokedAtIsNull(parentId)
                .forEach(LoginSession::revoke);

        auditLogger.log(admin, AuditAction.SUSPEND, TARGET_TYPE, parentId,
                "사용자 정지: %s (%s)".formatted(parent.getName(), reason));
        return get(parentId);
    }

    @Transactional
    public MemberDetail restore(AdminPrincipal admin, UUID parentId) {
        Parent parent = load(parentId);
        parent.restore();
        auditLogger.log(admin, AuditAction.RESTORE, TARGET_TYPE, parentId,
                "사용자 정지 해제: %s".formatted(parent.getName()));
        return get(parentId);
    }

    /**
     * 로그인 세션을 전부 끊는다. 계정은 그대로 두고 다시 로그인하게 만드는 조작이다.
     *
     * <p>기기 분실이나 계정 공유 신고에 쓴다. 정지와 달리 사용자는 다시 들어올 수 있다.
     */
    @Transactional
    public void revokeLoginSessions(AdminPrincipal admin, UUID parentId) {
        Parent parent = load(parentId);
        List<LoginSession> active = loginSessionRepository.findAllByParentIdAndRevokedAtIsNull(parentId);
        active.forEach(LoginSession::revoke);

        auditLogger.log(admin, AuditAction.REVOKE_SESSION, TARGET_TYPE, parentId,
                "로그인 세션 종료: %s (%d건)".formatted(parent.getName(), active.size()));
    }

    private Map<UUID, Story> loadStories(List<StorySession> sessions) {
        List<UUID> storyIds = sessions.stream().map(StorySession::getStoryId).distinct().toList();
        if (storyIds.isEmpty()) {
            return Map.of();
        }
        return storyRepository.findAllById(storyIds).stream()
                .collect(Collectors.toMap(Story::getId, Function.identity()));
    }

    private Parent load(UUID parentId) {
        return parentRepository.findById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
