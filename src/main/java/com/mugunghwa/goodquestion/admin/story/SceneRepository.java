package com.mugunghwa.goodquestion.admin.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SceneRepository extends JpaRepository<StoryScene, UUID> {

    List<StoryScene> findAllByStoryIdOrderBySceneOrderAsc(UUID storyId);

    Optional<StoryScene> findByStoryIdAndSceneOrder(UUID storyId, short sceneOrder);

    int countByStoryId(UUID storyId);

    /** 장면이 하나도 없으면 -1을 준다. 그래야 다음 순서가 0이 된다. */
    @Query("select coalesce(max(s.sceneOrder), -1) from StoryScene s where s.story.id = :storyId")
    short findMaxSceneOrder(@Param("storyId") UUID storyId);

    /** 캐릭터를 지우기 전에 그 캐릭터를 쓰는 장면이 있는지 본다. */
    boolean existsByCharacterId(UUID characterId);
}
