package com.novelkeep.novel.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "episode",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_episode_story_part_number",
                columnNames = {"story_part_id", "episode_number"}
        )
)
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_part_id", nullable = false)
    private StoryPart storyPart;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EpisodeStatus status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Episode() {
    }

    public static Episode create(
            Integer episodeNumber,
            String title,
            String content,
            EpisodeStatus status
    ) {
        Episode episode = new Episode();
        episode.episodeNumber = episodeNumber;
        episode.title = title;
        episode.content = content;
        episode.characterCount = countCharacters(content);
        episode.status = status;
        return episode;
    }

    void assignStoryPart(StoryPart storyPart) {
        this.storyPart = storyPart;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (status == EpisodeStatus.PUBLISHED) {
            this.publishedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.characterCount = countCharacters(content);
        this.updatedAt = LocalDateTime.now();
        if (status == EpisodeStatus.PUBLISHED && publishedAt == null) {
            this.publishedAt = updatedAt;
        }
    }

    private static int countCharacters(String content) {
        if (content == null) {
            return 0;
        }
        return (int) content.replace("\r", "").replace("\n", "").codePoints().count();
    }

    public Long getId() {
        return id;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
