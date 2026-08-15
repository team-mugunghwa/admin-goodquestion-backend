package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.CreateRequest;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.NoticeDetail;
import com.mugunghwa.goodquestion.admin.notice.dto.NoticeDtos.UpdateRequest;
import com.mugunghwa.goodquestion.admin.support.AdminFixture;
import com.mugunghwa.goodquestion.admin.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class NoticeServiceTest {

    @Autowired NoticeService noticeService;
    @Autowired AdminFixture adminFixture;

    private AdminPrincipal admin;

    @BeforeEach
    void setUp() {
        admin = adminFixture.createAdmin();
    }

    @Test
    @DisplayName("상태를 비우고 만들면 비공개로 저장된다")
    void defaultsToDraft() {
        NoticeDetail created = noticeService.create(admin,
                new CreateRequest("초안 공지", "본문", null, false, null));

        assertThat(created.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(created.publishedAt()).isNull();
        assertThat(created.authorName()).isEqualTo("테스트관리자");
    }

    @Test
    @DisplayName("공개로 바꾸면 공개 시각이 채워지고, 이후 수정에도 그 시각은 유지된다")
    void publishStampsTimeOnce() {
        NoticeDetail created = noticeService.create(admin,
                new CreateRequest("공지", "본문", NoticeCategory.UPDATE, false, null));

        NoticeDetail published = noticeService.update(admin, created.id(),
                new UpdateRequest(null, null, null, null, ContentStatus.PUBLISHED));
        assertThat(published.publishedAt()).isNotNull();

        // 오타 하나 고쳤다고 공개 시각이 갱신되면 사용자 목록에서 새 공지처럼 맨 위로 올라온다.
        NoticeDetail edited = noticeService.update(admin, created.id(),
                new UpdateRequest("공지(수정)", null, null, null, null));
        assertThat(edited.publishedAt()).isEqualTo(published.publishedAt());
        assertThat(edited.status()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    @DisplayName("목록은 고정 공지를 먼저 보여준다")
    void pinnedFirst() {
        noticeService.create(admin, new CreateRequest("일반 공지", "본문", null, false, ContentStatus.PUBLISHED));
        noticeService.create(admin, new CreateRequest("고정 공지", "본문", null, true, ContentStatus.PUBLISHED));

        var page = noticeService.list(ContentStatus.PUBLISHED, null, 0, 20);
        assertThat(page.content().getFirst().pinned()).isTrue();
    }
}
