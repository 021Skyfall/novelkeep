package com.novelkeep.funding.dto;

public record FundingActionResult(
        boolean success,
        String message,
        Long campaignId,
        Long partId,
        Long novelId,
        int achievementPercent,
        int currentQuantity,
        int targetQuantity,
        String closeOutcome,
        boolean awaitingApproval
) {
    public static FundingActionResult ok(
            String message,
            Long campaignId,
            Long partId,
            Long novelId,
            int achievementPercent,
            int currentQuantity,
            int targetQuantity
    ) {
        return new FundingActionResult(
                true, message, campaignId, partId, novelId,
                achievementPercent, currentQuantity, targetQuantity,
                null, false
        );
    }

    public static FundingActionResult closeOk(
            String message,
            FundingCampaignSnapshot campaign,
            boolean fundingSuccess,
            boolean awaitingApproval
    ) {
        return new FundingActionResult(
                true,
                message,
                campaign.campaignId(),
                campaign.partId(),
                campaign.novelId(),
                campaign.achievementPercent(),
                campaign.currentQuantity(),
                campaign.targetQuantity(),
                fundingSuccess ? "SUCCESS" : "FAILED",
                awaitingApproval
        );
    }

    public static FundingActionResult fail(String message) {
        return new FundingActionResult(false, message, null, null, null, 0, 0, 0, null, false);
    }

    public record FundingCampaignSnapshot(
            Long campaignId,
            Long partId,
            Long novelId,
            int achievementPercent,
            int currentQuantity,
            int targetQuantity
    ) {
    }
}
