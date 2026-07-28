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

    public Integer getPartNumber() {
        return partNumber;
    }

    public String getTitle() {
        return title;
    }

    public StoryPartStatus getStatus() {
        return status;
    }
}
