package com.novelkeep.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.novelkeep.order.dto.BookOrderBatchRow;

public record AdminDashboardStats(
        long novelCount,
        long publishedEpisodeCount,
        long openFundingCount,
        long successFundingCount,
        long failedFundingCount,
        BigDecimal paidAmountSum,
        BigDecimal refundedAmountSum,
        Map<String, Long> orderCountByStatus,
        int successRatePercent,
        long pendingOrderCount,
        List<BookOrderBatchRow> recentOrders
) {
}
