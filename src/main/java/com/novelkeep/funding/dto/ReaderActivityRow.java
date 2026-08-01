package com.novelkeep.funding.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.domain.FundingParticipation;
import com.novelkeep.funding.domain.FundingPaymentStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;

public record ReaderActivityRow(
        Long participationId,
        Long campaignId,
        Long novelId,
        Long orderId,
        String novelTitle,
        String partLabel,
        int quantity,
        BigDecimal paidAmount,
        ReaderActivitySearchCriteria.Tab tab,
        FundingCampaignStatus campaignStatus,
        String campaignStatusLabel,
        FundingPaymentStatus paymentStatus,
        BookOrderStatus orderStatus,
        String orderStatusLabel,
        String statusNote,
        int achievementPercent,
        int currentQuantity,
        int targetQuantity,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        LocalDateTime orderedAt,
        LocalDateTime endAt,
        boolean awaitingApproval,
        boolean approved,
        boolean canRefund
    ) {

    public static ReaderActivityRow fromParticipation(
            FundingParticipation participation,
            BookOrder order
    ) {
        FundingCampaign campaign = participation.getCampaign();
        StoryPart part = campaign.getStoryPart();
        Novel novel = part.getNovel();
        String partLabel = novel.isMultiPart()
                ? part.getPartNumber() + "부 · " + part.getTitle()
                : "본편";
        ReaderActivitySearchCriteria.Tab tab = resolveTab(participation, order);
        BookOrderStatus orderStatus = order == null ? null : order.getStatus();
        boolean canRefund = campaign.getStatus() == FundingCampaignStatus.OPEN
                && participation.getPaymentStatus() == FundingPaymentStatus.PAID_MOCK;
        return new ReaderActivityRow(
                participation.getId(),
                campaign.getId(),
                novel.getId(),
                order == null ? null : order.getId(),
                novel.getTitle(),
                partLabel,
                participation.getQuantity(),
                participation.getMockPaidAmount(),
                tab,
                campaign.getStatus(),
                campaign.getStatus().getDisplayName(),
                participation.getPaymentStatus(),
                orderStatus,
                orderStatus == null ? null : orderStatus.getDisplayName(),
                resolveStatusNote(campaign, participation, orderStatus),
                campaign.achievementPercent(),
                campaign.getCurrentQuantity(),
                campaign.getTargetQuantity(),
                participation.getPaidAt(),
                participation.getRefundedAt(),
                order == null ? null : order.getOrderedAt(),
                campaign.getEndAt(),
                campaign.isAwaitingApproval(),
                campaign.isApproved(),
                canRefund
        );
    }

    public static ReaderActivitySearchCriteria.Tab resolveTab(
            FundingParticipation participation,
            BookOrder order
    ) {
        if (participation.getPaymentStatus() == FundingPaymentStatus.REFUNDED_MOCK) {
            return ReaderActivitySearchCriteria.Tab.REFUND;
        }
        if (order != null) {
            return ReaderActivitySearchCriteria.Tab.ORDER;
        }
        return ReaderActivitySearchCriteria.Tab.ACTIVE;
    }

    private static String resolveStatusNote(
            FundingCampaign campaign,
            FundingParticipation participation,
            BookOrderStatus orderStatus
    ) {
        if (orderStatus != null) {
            return orderStatus.progressNote();
        }
        if (participation.getPaymentStatus() == FundingPaymentStatus.REFUNDED_MOCK) {
            if (campaign.getStatus() == FundingCampaignStatus.FAILED) {
                return "실패 승인 완료 · 환불이 반영되었습니다.";
            }
            return "환불이 완료되었습니다.";
        }
        if (campaign.getStatus() == FundingCampaignStatus.SUCCESS && campaign.isAwaitingApproval()) {
            return "성공 마감 · 운영자 승인 대기 중입니다. 성공 마감 후 환불은 불가합니다.";
        }
        if (campaign.getStatus() == FundingCampaignStatus.SUCCESS && campaign.isApproved()) {
            return "성공 마감 · 승인 완료. 환불은 불가합니다.";
        }
        if (campaign.getStatus() == FundingCampaignStatus.FAILED && campaign.isAwaitingApproval()) {
            return "실패 마감 · 운영자 승인 대기 중입니다. 승인 후 환불됩니다.";
        }
        if (campaign.getStatus() == FundingCampaignStatus.OPEN) {
            return "펀딩 중 · 결제 완료. 펀딩 중에는 환불할 수 있습니다.";
        }
        return "결제 완료";
    }
}
