package com.novelkeep.order.dto;

import java.time.LocalDateTime;

import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.StoryPart;

public record BookOrderRow(
        Long id,
        Long novelId,
        Long campaignId,
        String novelTitle,
        String partLabel,
        int quantity,
        BookOrderStatus status,
        String statusLabel,
        LocalDateTime orderedAt,
        boolean canAdvance,
        String nextStatusLabel
) {

    public static BookOrderRow from(BookOrder order) {
        var campaign = order.getParticipation().getCampaign();
        StoryPart part = campaign.getStoryPart();
        Novel novel = part.getNovel();
        String partLabel = novel.isMultiPart()
                ? part.getPartNumber() + "부 · " + part.getTitle()
                : "본편";
        BookOrderStatus next = order.getStatus().next();
        return new BookOrderRow(
                order.getId(),
                novel.getId(),
                campaign.getId(),
                novel.getTitle(),
                partLabel,
                order.getQuantity(),
                order.getStatus(),
                order.getStatus().getDisplayName(),
                order.getOrderedAt(),
                next != null,
                next == null ? null : next.getDisplayName()
        );
    }
}
