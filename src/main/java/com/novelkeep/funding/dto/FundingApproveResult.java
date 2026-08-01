package com.novelkeep.funding.dto;

import com.novelkeep.funding.domain.FundingCampaign;

public record FundingApproveResult(
        boolean success,
        int affectedCount,
        int totalQuantity,
        FundingCampaign campaign
) {
}
