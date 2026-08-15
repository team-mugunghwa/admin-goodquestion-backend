package com.mugunghwa.goodquestion.admin.story;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterRepository extends JpaRepository<StoryCharacter, UUID> {

    List<StoryCharacter> findAllByStoryIdOrderByCharacterKeyAsc(UUID storyId);

    boolean existsByStoryIdAndCharacterKey(UUID storyId, String characterKey);
}
