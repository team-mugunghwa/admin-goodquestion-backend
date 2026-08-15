package com.mugunghwa.goodquestion.admin.guide.dto;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import com.mugunghwa.goodquestion.admin.guide.Guide;
import com.mugunghwa.goodquestion.admin.guide.GuideCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class GuideDtos {

    private GuideDtos() {
    }

    public record CreateRequest(
            GuideCategory category,
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            /** 비우면 해당 카테고리 맨 아래에 붙는다. */
            Short displayOrder,
            ContentStatus status
    ) {
    }

    public record UpdateRequest(
            GuideCategory category,
            @Size(max = 200) String title,
            String content,
            Short displayOrder,
            ContentStatus status
    ) {
    }

    /**
     * 순서 일괄 변경. 화면에서 드래그로 정렬한 결과를 그대로 보낸다.
     *
     * @param guideIds 원하는 순서대로 나열한 문서 id. 배열의 위치가 곧 순서다.
     */
    public record ReorderRequest(
            GuideCategory category,
            @NotEmpty List<UUID> guideIds
    ) {
    }

    public record GuideResponse(
            UUID id,
            GuideCategory category,
            String title,
            String content,
            short displayOrder,
            ContentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static GuideResponse from(Guide guide) {
            return new GuideResponse(guide.getId(), guide.getCategory(), guide.getTitle(),
                    guide.getContent(), guide.getDisplayOrder(), guide.getStatus(),
                    guide.getCreatedAt(), guide.getUpdatedAt());
        }
    }
}
