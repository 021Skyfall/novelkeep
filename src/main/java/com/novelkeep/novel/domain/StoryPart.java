package com.novelkeep.novel.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "story_part",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_story_part_novel_number",
                columnNames = {"novel_id", "part_number"}
        )
)
public class StoryPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(name = "part_number", nullable = false)
    private Integer partNumber;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoryPartStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "storyPart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("episodeNumber DESC")
    private List<Episode> episodes = new ArrayList<>();

    protected StoryPart() {
    }

    public static StoryPart create(Integer partNumber, String title, StoryPartStatus status) {
        StoryPart part = new StoryPart();
        part.partNumber = partNumber;
        part.title = title;
        part.status = status;
        return part;
    }

    void assignNovel(Novel novel) {
        this.novel = novel;
    }

    public void addEpisode(Episode episode) {
        episodes.add(episode);
        episode.assignStoryPart(this);
    }

    public void removeEpisode(Episode episode) {
        Long episodeId = episode.getId();
        episodes.removeIf(existing -> episodeId != null && episodeId.equals(existing.getId()));
    }

    public void update(String title, StoryPartStatus status) {
        this.title = title;
        this.status = status;
    }

    public void changePartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public boolean hasEpisodes() {
        return !episodes.isEmpty();
    }

    public boolean allEpisodesPublished() {
        return hasEpisodes() && episodes.stream()
                .allMatch(episode -> episode.getStatus() == EpisodeStatus.PUBLISHED);
    }

    public int totalCharacterCount() {
        return episodes.stream()
                .mapToInt(Episode::getCharacterCount)
                .sum();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Novel getNovel() {
        return novel;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public String getTitle() {
        return title;
    }

    public StoryPartStatus getStatus() {
        return status;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
