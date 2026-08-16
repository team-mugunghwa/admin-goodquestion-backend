package com.mugunghwa.goodquestion.admin.support.dto;

import com.mugunghwa.goodquestion.admin.support.Inquiry;
import com.mugunghwa.goodquestion.admin.support.InquiryAnswer;
import com.mugunghwa.goodquestion.admin.support.InquiryNote;
import com.mugunghwa.goodquestion.admin.support.InquiryCategory;
import com.mugunghwa.goodquestion.admin.support.InquiryStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SupportDtos {

    private SupportDtos() {
    }

    public record AnswerRequest(@NotBlank String content) {
    }

    public record AnswerResponse(
            UUID id,
            String adminName,
            String content,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static AnswerResponse from(InquiryAnswer answer) {
            return new AnswerResponse(answer.getId(), answer.getAdminName(), answer.getContent(),
                    answer.getCreatedAt(), answer.getUpdatedAt());
        }
    }

    /**
     * 목록 한 줄.
     *
     * @param parentName 작성자. 문의는 사용자별로 맥락이 이어지는 경우가 많아
     *                   목록에서 이름이 보여야 같은 사람의 문의를 알아본다.
     * @param answered   답변 여부. status로도 알 수 있지만 화면이 배지 하나로 쓰기 좋게 따로 준다.
     */
    public record InquirySummary(
            UUID id,
            UUID parentId,
            String parentName,
            String parentEmail,
            InquiryCategory category,
            String title,
            InquiryStatus status,
            boolean answered,
            OffsetDateTime answeredAt,
            OffsetDateTime createdAt,
            /** 담당자가 없으면 null. 목록에서 "누가 잡고 있는가"를 보여 준다. */
            String assigneeEmail
    ) {
    }

    public record InquiryDetail(
            UUID id,
            UUID parentId,
            String parentName,
            String parentEmail,
            InquiryCategory category,
            String title,
            String content,
            InquiryStatus status,
            OffsetDateTime answeredAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            /** 아직 답변이 없으면 null. */
            AnswerResponse answer,
            /** 담당자가 없으면 null. */
            String assigneeEmail,
            /** 내부 메모. 오래된 것부터라 처리 과정이 시간 순서로 읽힌다. */
            List<NoteResponse> notes
    ) {
        public static InquiryDetail of(Inquiry inquiry, String parentName, String parentEmail,
                                       InquiryAnswer answer, String assigneeEmail,
                                       List<NoteResponse> notes) {
            return new InquiryDetail(inquiry.getId(), inquiry.getParentId(), parentName, parentEmail,
                    inquiry.getCategory(), inquiry.getTitle(), inquiry.getContent(),
                    inquiry.getStatus(), inquiry.getAnsweredAt(), inquiry.getCreatedAt(),
                    inquiry.getUpdatedAt(),
                    answer == null ? null : AnswerResponse.from(answer),
                    assigneeEmail, notes);
        }
    }

    public record NoteRequest(@NotBlank String body) {
    }

    public record NoteResponse(
            UUID id,
            String authorEmail,
            String body,
            OffsetDateTime createdAt
    ) {
        public static NoteResponse from(InquiryNote note) {
            return new NoteResponse(note.getId(), note.getAuthorEmail(),
                    note.getBody(), note.getCreatedAt());
        }
    }

    /** 담당자 상태. 지정과 해제 응답이 같은 모양을 쓴다. */
    public record AssigneeResponse(String assigneeEmail) {
    }
}
