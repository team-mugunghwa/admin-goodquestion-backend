package com.mugunghwa.goodquestion.admin.notice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NoticeRevisionRepository extends JpaRepository<NoticeRevision, UUID> {

    /** 최신이 위로. 화면이 "방금 전 내용"부터 보여 준다. */
    List<NoticeRevision> findAllByNoticeIdOrderBySeqDesc(UUID noticeId);

    /**
     * 최신 [keep]개만 남기고 지운다.
     *
     * <p>공지를 자주 고치면 이력이 무한히 쌓인다. 오래된 이력은 되돌릴 일도
     * 없으므로 개수로 자른다. 부질의의 limit 는 JPQL 에 없어서 네이티브로 쓴다.
     */
    @Modifying
    @Query(value = """
            delete from admin_notice_revisions
            where notice_id = :noticeId
              and id not in (
                  select id from admin_notice_revisions
                  where notice_id = :noticeId
                  order by seq desc
                  limit :keep)
            """, nativeQuery = true)
    void deleteBeyondLatest(UUID noticeId, int keep);
}
