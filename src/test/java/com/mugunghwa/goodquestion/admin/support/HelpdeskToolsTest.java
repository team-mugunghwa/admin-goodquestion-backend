package com.mugunghwa.goodquestion.admin.support;

import com.mugunghwa.goodquestion.admin.global.security.AdminPrincipal;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquiryDetail;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.NoteRequest;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.NoteResponse;
import com.mugunghwa.goodquestion.admin.support.dto.SupportDtos.InquirySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 고객센터 운영 도구 - 담당자, 내부 메모.
 */
@IntegrationTest
@Transactional
class HelpdeskToolsTest {

    @Autowired SupportService supportService;
    @Autowired TestFixture fixture;

    private AdminPrincipal me;
    private AdminPrincipal colleague;
    private UUID inquiryId;

    @BeforeEach
    void seed() {
        me = fixture.createAdmin();
        colleague = fixture.createAdmin();
        UUID parentId = fixture.createParent("김보호자");
        inquiryId = fixture.createInquiry(parentId, "별가루가 안 들어와요");
    }

    @Test
    @DisplayName("담당을 잡으면 목록과 상세에 담당자가 보인다")
    void assignShowsUpEverywhere() {
        supportService.assignToMe(me, inquiryId);

        InquiryDetail detail = supportService.get(inquiryId);
        assertThat(detail.assigneeEmail()).isEqualTo(me.email());

        var page = supportService.list(null, null, null, 0, 50);
        InquirySummary summary = page.content().stream()
                .filter(row -> row.id().equals(inquiryId)).findFirst().orElseThrow();
        assertThat(summary.assigneeEmail()).isEqualTo(me.email());
    }

    @Test
    @DisplayName("이미 잡힌 문의도 넘겨받을 수 있다")
    void takeoverReplacesAssignee() {
        // 담당자가 자리를 비웠을 때 다른 사람이 넘겨받을 길이 있어야 한다.
        supportService.assignToMe(colleague, inquiryId);
        supportService.assignToMe(me, inquiryId);

        assertThat(supportService.get(inquiryId).assigneeEmail()).isEqualTo(me.email());
    }

    @Test
    @DisplayName("담당 해제하면 비어 있는 상태로 돌아간다")
    void unassignClears() {
        supportService.assignToMe(me, inquiryId);
        supportService.unassign(me, inquiryId);

        assertThat(supportService.get(inquiryId).assigneeEmail()).isNull();
    }

    @Test
    @DisplayName("담당이 없는 문의는 목록에서 null 로 나온다")
    void unassignedIsNull() {
        var page = supportService.list(null, null, null, 0, 50);
        InquirySummary summary = page.content().stream()
                .filter(row -> row.id().equals(inquiryId)).findFirst().orElseThrow();
        assertThat(summary.assigneeEmail()).isNull();
    }

    @Test
    @DisplayName("내부 메모가 시간 순서로 상세에 쌓인다")
    void notesAccumulateInOrder() {
        supportService.addNote(me, inquiryId, new NoteRequest("보호자께 전화드리기로 함"));
        supportService.addNote(colleague, inquiryId, new NoteRequest("통화 완료, 보상 지급 예정"));

        InquiryDetail detail = supportService.get(inquiryId);
        assertThat(detail.notes()).hasSize(2);
        // 처리 과정이 시간 순서로 읽혀야 한다.
        assertThat(detail.notes().get(0).body()).contains("전화드리기로");
        assertThat(detail.notes().get(0).authorEmail()).isEqualTo(me.email());
        assertThat(detail.notes().get(1).authorEmail()).isEqualTo(colleague.email());
    }

    @Test
    @DisplayName("메모 본문의 양쪽 공백은 지워진다")
    void trimsNoteBody() {
        NoteResponse note = supportService.addNote(me, inquiryId,
                new NoteRequest("  메모  "));
        assertThat(note.body()).isEqualTo("메모");
    }
}
