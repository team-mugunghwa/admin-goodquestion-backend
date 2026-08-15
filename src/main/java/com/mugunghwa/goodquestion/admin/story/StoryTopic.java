package com.mugunghwa.goodquestion.admin.story;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** 이야기와 주제의 연결. {@code @ManyToMany} 대신 명시적 매핑 엔티티를 쓴다. */
@Entity
@Table(name = "story_topics")
@IdClass(StoryTopic.Pk.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryTopic {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    public StoryTopic(Story story, Topic topic) {
        this.story = story;
        this.topic = topic;
    }

    @NoArgsConstructor
    public static class Pk implements Serializable {
        private UUID story;
        private UUID topic;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(story, pk.story) && Objects.equals(topic, pk.topic);
        }

        @Override
        public int hashCode() {
            return Objects.hash(story, topic);
        }
    }
}
