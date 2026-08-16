package com.mugunghwa.goodquestion.admin.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface NoticeScheduleRepository extends JpaRepository<NoticeSchedule, UUID> {

    List<NoticeSchedule> findAllByPublishAtLessThanEqual(OffsetDateTime now);
}
