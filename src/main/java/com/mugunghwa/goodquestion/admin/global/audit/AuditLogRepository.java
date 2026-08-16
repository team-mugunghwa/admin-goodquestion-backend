package com.mugunghwa.goodquestion.admin.global.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 필터를 겹쳐 거는 조회.
     *
     * <p>비어 있다는 뜻으로 null 대신 빈 문자열을 받는다. {@code :param is null} 을
     * 조건에 넣으면 PostgreSQL 이 파라미터 타입을 정하지 못해 실패한 전례가 있다.
     * 조작 종류는 필터가 없을 때 전체 목록을 넘긴다 - 같은 이유로 null 을 피한다.
     */
    @Query("""
            select a from AuditLog a
            where (:targetType = '' or a.targetType = :targetType)
              and (:adminEmail = '' or lower(a.adminEmail) like concat('%', lower(:adminEmail), '%'))
              and a.action in :actions
              and a.createdAt >= :from
              and a.createdAt < :to
            order by a.createdAt desc
            """)
    Page<AuditLog> search(String targetType,
                          String adminEmail,
                          Collection<AuditAction> actions,
                          OffsetDateTime from,
                          OffsetDateTime to,
                          Pageable pageable);
}
