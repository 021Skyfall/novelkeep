package com.novelkeep.order.domain;

import java.time.LocalDateTime;

import com.novelkeep.funding.domain.FundingParticipation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "book_order",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_book_order_participation",
                columnNames = {"funding_participation_id"}
        )
)
public class BookOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funding_participation_id", nullable = false)
    private FundingParticipation participation;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookOrderStatus status;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BookOrder() {
    }

    public static BookOrder fromParticipation(
            FundingParticipation participation,
            BookOrderStatus status,
            LocalDateTime orderedAt
    ) {
        BookOrder order = new BookOrder();
        order.participation = participation;
        order.quantity = participation.getQuantity();
        order.status = status;
        order.orderedAt = orderedAt;
        if (status == BookOrderStatus.COMPLETED) {
            order.completedAt = orderedAt.plusDays(3);
        }
        return order;
    }

    public void advanceStatus() {
        BookOrderStatus next = status.next();
        if (next == null) {
            throw new IllegalStateException("이미 제작 완료된 주문입니다.");
        }
        this.status = next;
        if (next == BookOrderStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public FundingParticipation getParticipation() {
        return participation;
    }

    public int getQuantity() {
        return quantity;
    }

    public BookOrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
