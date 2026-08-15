package com.mugunghwa.goodquestion.admin.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findAllByParentIdOrderByCreatedAtAsc(UUID parentId);

    /** 목록 화면이 보호자마다 아이 수를 따로 세지 않도록 한 번에 가져온다. */
    List<Child> findAllByParentIdIn(List<UUID> parentIds);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime from);
}
