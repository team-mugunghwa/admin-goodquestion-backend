package com.mugunghwa.goodquestion.admin.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface StorySessionRepository extends JpaRepository<StorySession, UUID> {

    Page<StorySession> findAllByChildIdInOrderByLastActivityAtDesc(List<UUID> childIds,
                                                                   Pageable pageable);

    long countByStartedAtGreaterThanEqual(OffsetDateTime from);

    long countByStatus(String status);

    /** 확인이 필요한 세션. 인덱스가 이 조건에 맞춰져 있다. */
    Page<StorySession> findAllBySafetyFlaggedTrueOrderByStartedAtDesc(Pageable pageable);
}
