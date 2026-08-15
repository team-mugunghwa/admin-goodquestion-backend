package com.mugunghwa.goodquestion.admin.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ParentRepository extends JpaRepository<Parent, UUID> {

    /**
     * 이름 또는 이메일로 찾는다. 검색어를 빈 문자열로 받는 이유는 공지 목록과 같다 -
     * PostgreSQL이 타입 없는 null을 bytea로 추론해 lower() 안에서 쿼리가 통째로 실패한다.
     *
     * <p>이메일이 null인 계정(소셜 로그인)이 있어서 coalesce로 빈 문자열을 채운다.
     * null에 like를 걸면 그 행은 무조건 빠지는데, 검색어가 비었을 때는 전체가 나와야 한다.
     */
    @Query("""
            select p from Parent p
            where (:status is null or p.status = :status)
              and (lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(p.email, '')) like lower(concat('%', :keyword, '%')))
            order by p.createdAt desc
            """)
    Page<Parent> search(@Param("status") ParentStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime from);

    List<Parent> findAllByIdIn(List<UUID> ids);
}
