package com.mugunghwa.goodquestion.admin.notice.dto;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.notice.Notice;
import com.mugunghwa.goodquestion.admin.notice.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NoticeDtos {

    private NoticeDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            NoticeCategory category,
            boolean pinned,
            /** 비우면 DRAFT로 저장된다. 실수로 바로 공개되지 않게 기본값을 비공개로 둔다. */
            ContentStatus status
    ) {
    }

    /** 전부 선택이다. 보낸 항목만 바뀐다. */
    public record UpdateRequest(
            @Size(max = 200) String title,
            String content,
            NoticeCategory category,
            Boolean pinned,
            ContentStatus status
    ) {
    }

    /** 목록용. 본문은 싣지 않는다 - 공지 본문은 길고 목록에서 쓰지 않는다. */
    public record NoticeSummary(
            UUID id,
            String title,
            NoticeCategory category,
            boolean pinned,
            ContentStatus status,
            OffsetDateTime publishedAt,
            int viewCount,
            String authorName,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static NoticeSummary from(Notice notice) {
            return new NoticeSummary(notice.getId(), notice.getTitle(), notice.getCategory(),
                    notice.isPinned(), notice.getStatus(), notice.getPublishedAt(),
                    notice.getViewCount(), notice.getAuthorName(),
                    notice.getCreatedAt(), notice.getUpdatedAt());
        }
    }

    public record NoticeDetail(
            UUID id,
            String title,
            String content,
            NoticeCategory category,
            boolean pinned,
            ContentStatus status,
            OffsetDateTime publishedAt,
            int viewCount,
            String authorName,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            /** 예약 공개 시각. 걸려 있지 않으면 null 이다. */
            OffsetDateTime scheduledPublishAt
    ) {
        public static NoticeDetail from(Notice notice, OffsetDateTime scheduledPublishAt) {
            return new NoticeDetail(notice.getId(), notice.getTitle(), notice.getContent(),
                    notice.getCategory(), notice.isPinned(), notice.getStatus(),
                    notice.getPublishedAt(), notice.getViewCount(), notice.getAuthorName(),
                    notice.getCreatedAt(), notice.getUpdatedAt(), scheduledPublishAt);
        }
    }
}
