package com.novelkeep.order.dto;

import java.time.LocalDateTime;

import com.novelkeep.order.domain.BookOrderStatus;

public record BookOrderBatchRow(
        Long campaignId,
        Long novelId,
        Long minOrderId,
        String novelTitle,
        String partLabel,
        int totalQuantity,
        int orderCount,
        BookOrderStatus status,
        String statusLabel,
        LocalDateTime orderedAt,
        boolean canAdvance,
        String nextStatusLabel
) {
}
