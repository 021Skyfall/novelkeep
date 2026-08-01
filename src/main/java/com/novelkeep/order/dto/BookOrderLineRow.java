package com.novelkeep.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;

public record BookOrderLineRow(
        Long orderId,
        Long memberId,
        String memberLabel,
        int quantity,
        BigDecimal paidAmount,
        BookOrderStatus status,
        String statusLabel,
        LocalDateTime orderedAt,
        LocalDateTime paidAt
) {

    public static BookOrderLineRow from(BookOrder order) {
        var participation = order.getParticipation();
        var member = participation.getMember();
        return new BookOrderLineRow(
                order.getId(),
                member.getId(),
                member.resolveLabel(),
                order.getQuantity(),
                participation.getMockPaidAmount(),
                order.getStatus(),
                order.getStatus().getDisplayName(),
                order.getOrderedAt(),
                participation.getPaidAt()
        );
    }
}
