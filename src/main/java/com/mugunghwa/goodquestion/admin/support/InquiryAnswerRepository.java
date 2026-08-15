package com.mugunghwa.goodquestion.admin.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, UUID> {

    Optional<InquiryAnswer> findByInquiryId(UUID inquiryId);

    /** 목록 화면에서 문의 수만큼 쿼리가 나가지 않도록 한 번에 가져온다. */
    List<InquiryAnswer> findAllByInquiryIdIn(List<UUID> inquiryIds);
}
