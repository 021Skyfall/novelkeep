package com.novelkeep.funding.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.novelkeep.member.domain.Member;

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
        name = "funding_participation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_funding_participation_campaign_member",
                columnNames = {"funding_campaign_id", "member_id"}
        )
)
public class FundingParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funding_campaign_id", nullable = false)
    private FundingCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "mock_paid_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal mockPaidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private FundingPaymentStatus paymentStatus;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected FundingParticipation() {
    }

    public static FundingParticipation paid(
            FundingCampaign campaign,
            Member member,
            int quantity,
            LocalDateTime paidAt
    ) {
        if (quantity < 1) {
            throw new IllegalArgumentException("참여 수량은 1 이상이어야 합니다.");
        }
        FundingParticipation participation = new FundingParticipation();
        participation.campaign = campaign;
        participation.member = member;
        participation.quantity = quantity;
        participation.mockPaidAmount = campaign.getPriceAmount()
                .multiply(BigDecimal.valueOf(quantity));
        participation.paymentStatus = FundingPaymentStatus.PAID_MOCK;
        participation.paidAt = paidAt;
        return participation;
    }

    public void refundMock(LocalDateTime refundedAt) {
        if (paymentStatus != FundingPaymentStatus.PAID_MOCK) {
            throw new IllegalStateException("결제 완료 건만 환불할 수 있습니다.");
        }
        this.paymentStatus = FundingPaymentStatus.REFUNDED_MOCK;
        this.refundedAt = refundedAt != null ? refundedAt : LocalDateTime.now();
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

    public Long getId() {
        return id;
    }

    public FundingCampaign getCampaign() {
        return campaign;
    }

    public Member getMember() {
        return member;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getMockPaidAmount() {
        return mockPaidAmount;
    }

    public FundingPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
