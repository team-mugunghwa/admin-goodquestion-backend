package com.mugunghwa.goodquestion.admin.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StoryTopicRepository extends JpaRepository<StoryTopic, StoryTopic.Pk> {

    /**
     * 목록 화면이 이야기마다 주제를 따로 묻지 않도록 한 번에 가져온다.
     * fetch join이 없으면 주제 이름을 읽는 순간 이야기 수만큼 쿼리가 더 나간다.
     */
    @Query("select st from StoryTopic st join fetch st.topic where st.story.id in :storyIds")
    List<StoryTopic> findAllByStoryIds(@Param("storyIds") List<UUID> storyIds);

    void deleteAllByStoryId(UUID storyId);

    /** 이 주제를 쓰는 이야기 수. 주제를 지우기 전 확인 문구에 쓴다. */
    long countByTopicId(UUID topicId);
}
