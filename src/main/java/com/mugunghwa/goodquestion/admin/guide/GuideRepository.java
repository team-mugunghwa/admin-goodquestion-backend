package com.mugunghwa.goodquestion.admin.guide;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GuideRepository extends JpaRepository<Guide, UUID> {

    /**
     * 관리자 목록. 이용안내는 많아야 수십 건이고 순서를 보며 편집하는 화면이라
     * 페이징하지 않고 전부 내린다. 페이지를 넘겨 가며 순서를 맞출 수는 없다.
     */
    @Query("""
            select g from Guide g
            where (:category is null or g.category = :category)
              and (:status is null or g.status = :status)
            order by g.category asc, g.displayOrder asc, g.createdAt asc
            """)
    List<Guide> search(@Param("category") GuideCategory category,
                       @Param("status") ContentStatus status);

    /** 새 문서를 카테고리 맨 아래에 붙이기 위한 값. 비어 있으면 null이다. */
    @Query("select max(g.displayOrder) from Guide g where g.category = :category")
    Short findMaxDisplayOrder(@Param("category") GuideCategory category);

    long countByStatus(ContentStatus status);
}
