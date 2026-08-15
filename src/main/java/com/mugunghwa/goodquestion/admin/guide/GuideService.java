package com.mugunghwa.goodquestion.admin.guide;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.audit.AuditAction;
import com.mugunghwa.goodquestion.admin.global.audit.AuditLogger;
import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.error.ErrorCode;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.GuideResponse;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.ReorderRequest;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.UpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideService {

    private static final String TARGET_TYPE = "GUIDE";

    private final GuideRepository repository;
    private final AuditLogger auditLogger;

    public List<GuideResponse> list(GuideCategory category, ContentStatus status) {
        return repository.search(category, status).stream().map(GuideResponse::from).toList();
    }

    public GuideResponse get(UUID guideId) {
        return GuideResponse.from(load(guideId));
    }

    @Transactional
    public GuideResponse create(AdminPrincipal admin, CreateRequest request) {
        GuideCategory category = request.category() != null ? request.category() : GuideCategory.BASIC;
        Guide guide = repository.save(Guide.builder()
                .category(category)
                .title(request.title())
                .content(request.content())
                .displayOrder(request.displayOrder() != null
                        ? request.displayOrder() : nextDisplayOrder(category))
                .status(request.status())
                .build());

        auditLogger.log(admin, AuditAction.CREATE, TARGET_TYPE, guide.getId(),
                "이용안내 작성: %s (%s)".formatted(guide.getTitle(), guide.getCategory()));
        return GuideResponse.from(guide);
    }

    @Transactional
    public GuideResponse update(AdminPrincipal admin, UUID guideId, UpdateRequest request) {
        Guide guide = load(guideId);
        ContentStatus before = guide.getStatus();
        guide.update(request.category(), request.title(), request.content(),
                request.displayOrder(), request.status());

        boolean statusChanged = request.status() != null && request.status() != before;
        auditLogger.log(admin, statusChanged ? AuditAction.PUBLISH : AuditAction.UPDATE,
                TARGET_TYPE, guide.getId(),
                statusChanged
                        ? "이용안내 상태 변경: %s (%s -> %s)".formatted(guide.getTitle(), before, guide.getStatus())
                        : "이용안내 수정: %s".formatted(guide.getTitle()));
        return GuideResponse.from(guide);
    }

    /**
     * 순서 일괄 변경.
     *
     * <p>보낸 배열의 위치를 그대로 순서로 쓴다. 화면에서 드래그한 결과가 곧 최종 상태이므로
     * "위로 한 칸" 같은 상대 조작으로 받지 않는다 - 상대 조작은 여러 관리자가 동시에
     * 만졌을 때 어긋난 순서로 수렴한다.
     *
     * <p>보낸 목록에 없는 문서까지 그 카테고리 전체를 다시 번호 매긴다. 보낸 것만 0부터
     * 다시 매기면, 화면이 열린 사이에 다른 관리자가 추가한 문서와 순서 값이 겹쳐
     * 정렬 결과가 들쭉날쭉해진다. 빠진 문서는 기존 순서를 유지한 채 뒤에 붙는다.
     *
     * <p>존재하지 않는 id나 다른 카테고리의 문서가 섞여 오면 그것만 조용히 건너뛴다.
     * 화면이 보고 있던 목록과 서버 상태가 다를 수 있는데, 그때 전체를 400으로 되돌리면
     * 관리자는 순서를 처음부터 다시 잡아야 한다.
     */
    @Transactional
    public List<GuideResponse> reorder(AdminPrincipal admin, ReorderRequest request) {
        List<Guide> categoryGuides = repository.search(request.category(), null);
        Map<UUID, Guide> byId = categoryGuides.stream()
                .collect(Collectors.toMap(Guide::getId, Function.identity()));

        short order = 0;
        Set<UUID> placed = new LinkedHashSet<>();
        for (UUID guideId : request.guideIds()) {
            Guide guide = byId.get(guideId);
            if (guide == null || !placed.add(guideId)) continue;
            guide.changeOrder(order++);
        }
        // 요청에 빠진 문서들. search()가 기존 순서대로 주므로 그 상대 순서가 유지된다.
        for (Guide guide : categoryGuides) {
            if (placed.contains(guide.getId())) continue;
            guide.changeOrder(order++);
        }

        auditLogger.log(admin, AuditAction.UPDATE, TARGET_TYPE, (UUID) null,
                "이용안내 순서 변경: %s %d건".formatted(request.category(), placed.size()));
        return list(request.category(), null);
    }

    @Transactional
    public void delete(AdminPrincipal admin, UUID guideId) {
        Guide guide = load(guideId);
        String title = guide.getTitle();
        repository.delete(guide);
        auditLogger.log(admin, AuditAction.DELETE, TARGET_TYPE, guideId,
                "이용안내 삭제: %s".formatted(title));
    }

    private short nextDisplayOrder(GuideCategory category) {
        Short max = repository.findMaxDisplayOrder(category);
        return max == null ? 0 : (short) (max + 1);
    }

    private Guide load(UUID guideId) {
        return repository.findById(guideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이용안내를 찾을 수 없습니다."));
    }
}
