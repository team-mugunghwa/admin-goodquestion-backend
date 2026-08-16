package com.mugunghwa.goodquestion.admin.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InquiryNoteRepository extends JpaRepository<InquiryNote, UUID> {

    /** 오래된 것부터. 메모는 처리 과정의 기록이라 시간 순서대로 읽어야 맥락이 잡힌다. */
    List<InquiryNote> findAllByInquiryIdOrderByCreatedAtAsc(UUID inquiryId);
}
