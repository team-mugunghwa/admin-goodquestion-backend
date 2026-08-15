package com.mugunghwa.goodquestion.admin.notice;

import com.mugunghwa.goodquestion.admin.content.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    /**
     * 관리자 목록. 상태와 검색어는 둘 다 선택이다.
     *
     * <p>Specification이나 QueryDSL 대신 쿼리 하나로 두었다. 조건이 둘뿐이고 앞으로도
     * 크게 늘 것 같지 않은데, 그것들을 끌어오면 읽을 것만 늘어난다.
     *
     * <p>검색어를 {@code :keyword is null} 로 걸러내지 않고 빈 문자열을 받는다.
     * PostgreSQL은 타입이 안 붙은 null 파라미터를 bytea로 추론하는데, 그 값이
     * {@code lower()} 안에 들어가면 "function lower(bytea) does not exist"로 쿼리가
     * 통째로 실패한다. 빈 문자열이면 {@code like '%%'}가 되어 모두 통과하므로
     * 조건 분기 자체가 필요 없다.
     */
    @Query("""
            select n from Notice n
            where (:status is null or n.status = :status)
              and lower(n.title) like lower(concat('%', :keyword, '%'))
            order by n.pinned desc, n.createdAt desc
            """)
    Page<Notice> search(@Param("status") ContentStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

    long countByStatus(ContentStatus status);
}
