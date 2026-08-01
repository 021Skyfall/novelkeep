package com.novelkeep.funding.dto;

import com.novelkeep.funding.domain.FundingCampaign;

public record FundingCloseResult(
        boolean success,
        int paidCount,
        FundingCampaign campaign
) {
}
