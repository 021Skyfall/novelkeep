package com.novelkeep.funding.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;

import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;

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

    /** 성공/실패 마감 후 운영자 승인 시각. null이면 승인 대기. */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 작가 마감(승인 요청) 시각. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    protected FundingCampaign() {
    }

    public static FundingCampaign draft(
            StoryPart storyPart,
            int targetQuantity,
            BigDecimal priceAmount,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        requireEndAfterStart(startAt, endAt);
        FundingGuide.validateTarget(targetQuantity, priceAmount);
        FundingCampaign campaign = new FundingCampaign();
        campaign.storyPart = storyPart;
        campaign.status = FundingCampaignStatus.DRAFT;
        campaign.targetQuantity = targetQuantity;
        campaign.currentQuantity = 0;
        campaign.priceAmount = priceAmount;
        campaign.startAt = startAt;
        campaign.endAt = endAt;
        return campaign;
    }

    public static FundingCampaign open(
            StoryPart storyPart,
            int targetQuantity,
            int currentQuantity,
            BigDecimal priceAmount,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        requireEndAfterStart(startAt, endAt);
        FundingGuide.validateTarget(targetQuantity, priceAmount);
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

    private static void requireEndAfterStart(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("펀딩 종료일은 시작일보다 이후여야 합니다.");
        }
    }

    public void updateWhileOpen(int targetQuantity, BigDecimal priceAmount, LocalDateTime endAt) {
        if (status != FundingCampaignStatus.OPEN) {
            throw new IllegalStateException("진행 중인 펀딩만 수정할 수 있습니다.");
        }
        FundingGuide.validateTarget(targetQuantity, priceAmount);
        FundingGuide.validateEndSchedule(this.startAt, endAt, FundingGuide.nowKorea());
        this.targetQuantity = targetQuantity;
        this.priceAmount = priceAmount;
        this.endAt = endAt;
    }

    public void updateEditable(int targetQuantity, BigDecimal priceAmount, LocalDateTime startAt, LocalDateTime endAt) {
        if (status == FundingCampaignStatus.OPEN) {
            updateWhileOpen(targetQuantity, priceAmount, endAt);
            return;
        }
        if (status != FundingCampaignStatus.DRAFT) {
            throw new IllegalStateException("미진행 또는 진행 중인 펀딩만 수정할 수 있습니다.");
        }
        FundingGuide.validateCreateSchedule(startAt, endAt);
        FundingGuide.validateTarget(targetQuantity, priceAmount);
        this.targetQuantity = targetQuantity;
        this.priceAmount = priceAmount;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void openNow() {
        if (status != FundingCampaignStatus.DRAFT) {
            throw new IllegalStateException("미진행 상태의 펀딩만 시작할 수 있습니다.");
        }
        LocalDateTime now = FundingGuide.nowKorea();
        FundingGuide.validateEndSchedule(startAt.isAfter(now) ? now : startAt, endAt, now);
        this.status = FundingCampaignStatus.OPEN;
        if (this.startAt.isAfter(now)) {
            this.startAt = now;
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = FundingGuide.nowKorea();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = FundingGuide.nowKorea();
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

    public static int averageAchievementPercent(Collection<FundingCampaign> campaigns) {
        if (campaigns == null || campaigns.isEmpty()) {
            return 0;
        }
        double average = campaigns.stream()
                .mapToInt(FundingCampaign::achievementPercent)
                .average()
                .orElse(0d);
        return (int) Math.round(average);
    }

    public String displayTitle() {
        var novel = storyPart.getNovel();
        if (novel.isMultiPart()) {
            return novel.getTitle() + " " + storyPart.getPartNumber() + "부";
        }
        return novel.getTitle() + " 소장본";
    }

    public String partCompletionLabel() {
        if (!storyPart.getNovel().isMultiPart()) {
            return "본편";
        }
        return storyPart.getPartNumber() + "부 · " + storyPart.getStatus().getDisplayName();
    }

    public boolean canCancel() {
        return status == FundingCampaignStatus.OPEN && currentQuantity <= 0;
    }

    public boolean canClose() {
        return status == FundingCampaignStatus.OPEN && currentQuantity > 0;
    }

    public boolean isAwaitingApproval() {
        return (status == FundingCampaignStatus.SUCCESS || status == FundingCampaignStatus.FAILED)
                && approvedAt == null;
    }

    public boolean isApproved() {
        return approvedAt != null;
    }

    /**
     * 시드·정합용. 게이지(currentQuantity)를 실제 참여 합과 맞출 때 사용한다.
     */
    public void syncCurrentQuantity(int quantity) {
        this.currentQuantity = Math.max(0, quantity);
    }

    public boolean isWithinPeriod(LocalDateTime now) {
        if (now == null || startAt == null || endAt == null) {
            return false;
        }
        return !now.isBefore(startAt) && !now.isAfter(endAt);
    }

    public boolean isOpenForJoin(LocalDateTime now) {
        return status == FundingCampaignStatus.OPEN && isWithinPeriod(now);
    }

    public void recordParticipation(int quantity) {
        if (status != FundingCampaignStatus.OPEN) {
            throw new IllegalStateException("진행 중인 펀딩에만 참여할 수 있습니다.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("참여 수량은 1 이상이어야 합니다.");
        }
        this.currentQuantity += quantity;
    }

    public void closeAsSuccess() {
        if (status != FundingCampaignStatus.OPEN) {
            throw new IllegalStateException("진행 중인 펀딩만 마감할 수 있습니다.");
        }
        this.status = FundingCampaignStatus.SUCCESS;
        this.approvedAt = null;
        this.closedAt = FundingGuide.nowKorea();
    }

    public void closeAsFailed() {
        if (status != FundingCampaignStatus.OPEN) {
            throw new IllegalStateException("진행 중인 펀딩만 마감할 수 있습니다.");
        }
        this.status = FundingCampaignStatus.FAILED;
        this.approvedAt = null;
        this.closedAt = FundingGuide.nowKorea();
    }

    public void markApproved(LocalDateTime approvedAt) {
        if (status != FundingCampaignStatus.SUCCESS && status != FundingCampaignStatus.FAILED) {
            throw new IllegalStateException("성공 또는 실패로 마감된 펀딩만 승인할 수 있습니다.");
        }
        if (this.approvedAt != null) {
            throw new IllegalStateException("이미 승인된 펀딩입니다.");
        }
        this.approvedAt = approvedAt != null ? approvedAt : FundingGuide.nowKorea();
    }

    /** 운영자 거절 시 진행 중으로 되돌린다. */
    public void reopenAfterReject() {
        if (status != FundingCampaignStatus.SUCCESS && status != FundingCampaignStatus.FAILED) {
            throw new IllegalStateException("마감된 펀딩만 거절할 수 있습니다.");
        }
        if (this.approvedAt != null) {
            throw new IllegalStateException("이미 승인된 펀딩은 거절할 수 없습니다.");
        }
        this.status = FundingCampaignStatus.OPEN;
        this.closedAt = null;
        this.approvedAt = null;
    }

    public boolean isSuccessReady() {
        return storyPart != null
                && storyPart.getStatus() == StoryPartStatus.COMPLETED
                && currentQuantity >= targetQuantity;
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

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
