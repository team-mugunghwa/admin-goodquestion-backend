package com.mugunghwa.goodquestion.admin.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    /**
     * 관리자 목록.
     *
     * <p>정렬이 상태에 따라 갈린다. 미답변만 볼 때는 <b>오래된 순</b>이 맞다 - 가장 오래
     * 기다린 사람이 먼저 처리돼야 한다. 그 밖에는 최신순이 익숙하다. 정렬을 하나로
     * 고정하면 둘 중 하나는 매번 마지막 페이지까지 넘겨야 한다.
     */
    @Query("""
            select i from Inquiry i
            where (:status is null or i.status = :status)
              and (:category is null or i.category = :category)
              and lower(i.title) like lower(concat('%', :keyword, '%'))
            order by
                case when i.status = com.mugunghwa.goodquestion.admin.support.InquiryStatus.PENDING
                     then 0 else 1 end asc,
                case when i.status = com.mugunghwa.goodquestion.admin.support.InquiryStatus.PENDING
                     then i.createdAt end asc,
                i.createdAt desc
            """)
    Page<Inquiry> search(@Param("status") InquiryStatus status,
                         @Param("category") InquiryCategory category,
                         @Param("keyword") String keyword,
                         Pageable pageable);

    Page<Inquiry> findAllByParentIdOrderByCreatedAtDesc(UUID parentId, Pageable pageable);

    long countByStatus(InquiryStatus status);
}
