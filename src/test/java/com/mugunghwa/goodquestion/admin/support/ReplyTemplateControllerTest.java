package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.global.error.BusinessException;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.support.ReplyTemplateController.TemplateRequest;
import com.mugunghwa.goodquestion.admin.support.ReplyTemplateController.TemplateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 자주 쓰는 답변 템플릿. */
@IntegrationTest
@Transactional
class ReplyTemplateControllerTest {

    @Autowired ReplyTemplateController controller;
    @Autowired TestFixture fixture;

    private AdminPrincipal admin;

    @BeforeEach
    void seed() {
        admin = fixture.createAdmin();
    }

    @Test
    @DisplayName("만들고 고치고 지우는 흐름이 동작한다")
    void crudFlow() {
        TemplateResponse created = controller.create(admin,
                new TemplateRequest("환불 안내",
                        "{보호자} 님, 문의 주셔서 감사합니다. 환불은 3일 안에 처리됩니다."));
        assertThat(created.body()).contains("{보호자}");

        TemplateResponse updated = controller.update(admin, created.id(),
                new TemplateRequest("환불 안내 (개정)", "본문 수정"));
        assertThat(updated.title()).isEqualTo("환불 안내 (개정)");

        controller.delete(admin, created.id());
        assertThat(controller.list()).extracting(TemplateResponse::id)
                .doesNotContain(created.id());
    }

    @Test
    @DisplayName("최근에 손댄 템플릿이 목록 위로 온다")
    void recentlyTouchedFirst() {
        TemplateResponse first = controller.create(admin, new TemplateRequest("가", "본문"));
        TemplateResponse second = controller.create(admin, new TemplateRequest("나", "본문"));

        // 먼저 만든 것을 고치면 그것이 맨 위로 와야 한다.
        controller.update(admin, first.id(), new TemplateRequest("가 수정", "본문"));

        List<TemplateResponse> list = controller.list();
        int firstIndex = indexOf(list, first.id());
        int secondIndex = indexOf(list, second.id());
        assertThat(firstIndex).isLessThan(secondIndex);
    }

    @Test
    @DisplayName("없는 템플릿을 고치려 하면 거절한다")
    void rejectsUnknownId() {
        assertThatThrownBy(() -> controller.update(admin, UUID.randomUUID(),
                new TemplateRequest("제목", "본문")))
                .isInstanceOf(BusinessException.class);
    }

    private int indexOf(List<TemplateResponse> list, UUID id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) return i;
        }
        return -1;
    }
}
