package com.novelkeep.funding.domain;

public enum FundingCampaignStatus {

    DRAFT("초안"),
    OPEN("진행 중"),
    SUCCESS("성공"),
    FAILED("실패");

    private final String displayName;

    FundingCampaignStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
