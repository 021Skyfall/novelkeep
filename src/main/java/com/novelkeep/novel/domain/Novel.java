package com.novelkeep.novel.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.novelkeep.member.domain.Member;

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

@Entity
@Table(name = "novel")
public class Novel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "pen_name", nullable = false, length = 50)
    private String penName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NovelGenre genre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String synopsis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NovelStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'PUBLIC'")
    private NovelVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_mode", nullable = false, length = 20)
    private PartMode partMode;

    @Column(name = "recommendation_count", nullable = false, columnDefinition = "bigint not null default 0")
    private long recommendationCount;

    @OneToMany(mappedBy = "novel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("partNumber ASC")
    private List<StoryPart> parts = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Novel() {
    }

    public static Novel create(
            Member author,
            String title,
            String penName,
            NovelGenre genre,
            String synopsis,
            NovelStatus status,
            NovelVisibility visibility,
            PartMode partMode
    ) {
        Novel novel = new Novel();
        novel.author = author;
        novel.title = title;
        novel.penName = penName;
        novel.genre = genre;
        novel.synopsis = synopsis;
        novel.status = status;
        novel.visibility = visibility;
        novel.partMode = partMode;
        novel.recommendationCount = 0L;
        return novel;
    }

    public void increaseRecommendationCount() {
        this.recommendationCount += 1;
    }

    public void decreaseRecommendationCount() {
        if (this.recommendationCount > 0) {
            this.recommendationCount -= 1;
        }
    }

    public void addPart(StoryPart part) {
        parts.add(part);
        part.assignNovel(this);
    }

    public void update(
            String title,
            String penName,
            NovelGenre genre,
            String synopsis,
            NovelStatus status,
            NovelVisibility visibility
    ) {
        this.title = title;
        this.penName = penName;
        this.genre = genre;
        this.synopsis = synopsis;
        this.status = status;
        this.visibility = visibility;
    }

    public boolean hasCompletedPart() {
        return parts.stream().anyMatch(part -> part.getStatus() == StoryPartStatus.COMPLETED);
    }

    public Integer latestCompletedPartNumber() {
        return parts.stream()
                .filter(part -> part.getStatus() == StoryPartStatus.COMPLETED)
                .map(StoryPart::getPartNumber)
                .max(Integer::compareTo)
                .orElse(null);
    }

    public String latestCompletedPartLabel() {
        Integer partNumber = latestCompletedPartNumber();
        return partNumber == null ? null : partNumber + "부 완결";
    }

    public boolean isOwnedBy(Long memberId) {
        return author != null && author.getId().equals(memberId);
    }

    public boolean isReadableBy(Long memberId, boolean admin) {
        if (visibility == NovelVisibility.PUBLIC) {
            return true;
        }
        return admin || isOwnedBy(memberId);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.visibility == null) {
            this.visibility = NovelVisibility.PRIVATE;
        }
        if (this.recommendationCount < 0) {
            this.recommendationCount = 0L;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getPenName() {
        return penName;
    }

    public NovelGenre getGenre() {
        return genre;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public NovelStatus getStatus() {
        return status;
    }

    public NovelVisibility getVisibility() {
        return visibility;
    }

    public PartMode getPartMode() {
        return partMode;
    }

    public long getRecommendationCount() {
        return recommendationCount;
    }

    public List<StoryPart> getParts() {
        return parts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
