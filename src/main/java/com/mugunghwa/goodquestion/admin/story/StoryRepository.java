package com.mugunghwa.goodquestion.admin.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    /** 검색어를 빈 문자열로 받는 이유는 공지 목록과 같다(널 파라미터의 타입 추론 문제). */
    @Query("""
            select s from Story s
            where (:status is null or s.status = :status)
              and lower(s.title) like lower(concat('%', :keyword, '%'))
            order by s.createdAt desc
            """)
    Page<Story> search(@Param("status") StoryStatus status,
                       @Param("keyword") String keyword,
                       Pageable pageable);

    long countByStatus(StoryStatus status);

    /**
     * 이 이야기로 시작된 세션 수. 삭제해도 되는지 판단하는 데 쓴다.
     *
     * <p>세션 도메인 전체를 이야기 쪽에 들일 이유가 없어 개수만 세는 네이티브 쿼리로 둔다.
     */
    @Query(value = "select count(*) from story_sessions where story_id = :storyId",
            nativeQuery = true)
    long countSessions(@Param("storyId") UUID storyId);
}
