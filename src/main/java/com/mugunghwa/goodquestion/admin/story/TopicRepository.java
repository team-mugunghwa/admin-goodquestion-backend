package com.mugunghwa.goodquestion.admin.story;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByOrderByDisplayOrderAsc();

    Optional<Topic> findByName(String name);

    boolean existsByName(String name);
}
