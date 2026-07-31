package com.novelkeep.funding.dto;

public record FundingActionResult(
        boolean success,
        String message,
        Long campaignId,
        Long partId,
        Long novelId,
        int achievementPercent,
        int currentQuantity,
        int targetQuantity
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
                achievementPercent, currentQuantity, targetQuantity
        );
    }

    public static FundingActionResult fail(String message) {
        return new FundingActionResult(false, message, null, null, null, 0, 0, 0);
    }
}
