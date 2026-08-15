package com.mugunghwa.goodquestion.admin.guide;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.GuideResponse;
import com.mugunghwa.goodquestion.admin.guide.dto.GuideDtos.ReorderRequest;
import com.mugunghwa.goodquestion.admin.support.TestFixture;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class GuideServiceTest {

    @Autowired GuideService guideService;
    @Autowired TestFixture fixture;

    private AdminPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = fixture.createAdmin();
    }

    @Test
    @DisplayName("순서를 비우고 만들면 그 카테고리 맨 아래에 붙는다")
    void appendsToBottom() {
        GuideResponse first = guideService.create(admin,
                new CreateRequest(GuideCategory.TROUBLE, "첫 문서", "본문", null, null));
        GuideResponse second = guideService.create(admin,
                new CreateRequest(GuideCategory.TROUBLE, "둘째 문서", "본문", null, null));

        assertThat(second.displayOrder()).isGreaterThan(first.displayOrder());
    }

    @Test
    @DisplayName("순서 일괄 변경은 보낸 배열의 위치를 그대로 순서로 쓴다")
    void reorderUsesArrayPosition() {
        GuideResponse a = guideService.create(admin,
                new CreateRequest(GuideCategory.REWARD, "A", "본문", null, null));
        GuideResponse b = guideService.create(admin,
                new CreateRequest(GuideCategory.REWARD, "B", "본문", null, null));
        GuideResponse c = guideService.create(admin,
                new CreateRequest(GuideCategory.REWARD, "C", "본문", null, null));

        List<GuideResponse> reordered = guideService.reorder(admin,
                new ReorderRequest(GuideCategory.REWARD, List.of(c.id(), a.id(), b.id())));

        // 시드로 들어온 이용안내가 같은 카테고리에 있을 수 있어 상대 순서만 본다.
        assertThat(reordered).extracting(GuideResponse::title).containsSubsequence("C", "A", "B");
    }

    @Test
    @DisplayName("목록에 없는 id가 섞여 와도 나머지 순서는 반영된다")
    void reorderSkipsUnknownIds() {
        GuideResponse a = guideService.create(admin,
                new CreateRequest(GuideCategory.PLAY, "A", "본문", null, null));
        GuideResponse b = guideService.create(admin,
                new CreateRequest(GuideCategory.PLAY, "B", "본문", null, null));

        List<GuideResponse> reordered = guideService.reorder(admin,
                new ReorderRequest(GuideCategory.PLAY, List.of(b.id(), UUID.randomUUID(), a.id())));

        assertThat(reordered).extracting(GuideResponse::title).containsSubsequence("B", "A");
    }
}
