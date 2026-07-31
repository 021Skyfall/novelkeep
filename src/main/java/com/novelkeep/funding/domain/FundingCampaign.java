package com.novelkeep.funding.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.novelkeep.novel.domain.StoryPart;

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

@Entity
@Table(name = "funding_campaign")
public class FundingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_part_id", nullable = false)
    private StoryPart storyPart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingCampaignStatus status;

    @Column(name = "target_quantity", nullable = false)
    private int targetQuantity;

    @Column(name = "current_quantity", nullable = false)
    private int currentQuantity;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal priceAmount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected FundingCampaign() {
    }

    public static FundingCampaign open(
            StoryPart storyPart,
            int targetQuantity,
            int currentQuantity,
            BigDecimal priceAmount,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        FundingCampaign campaign = new FundingCampaign();
        campaign.storyPart = storyPart;
        campaign.status = FundingCampaignStatus.OPEN;
        campaign.targetQuantity = targetQuantity;
        campaign.currentQuantity = Math.max(0, currentQuantity);
        campaign.priceAmount = priceAmount;
        campaign.startAt = startAt;
        campaign.endAt = endAt;
        return campaign;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int achievementPercent() {
        if (targetQuantity <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(currentQuantity)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(targetQuantity), 0, RoundingMode.DOWN)
                .intValue();
    }

    public String displayTitle() {
        var novel = storyPart.getNovel();
        if (novel.isMultiPart()) {
            return novel.getTitle() + " " + storyPart.getPartNumber() + "권";
        }
        return novel.getTitle() + " 소장본";
    }

    public String partCompletionLabel() {
        if (!storyPart.getNovel().isMultiPart()) {
            return "본편";
        }
        return storyPart.getPartNumber() + "부 · " + storyPart.getStatus().getDisplayName();
    }

    public Long getId() {
        return id;
    }

    public StoryPart getStoryPart() {
        return storyPart;
    }

    public FundingCampaignStatus getStatus() {
        return status;
    }

    public int getTargetQuantity() {
        return targetQuantity;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
