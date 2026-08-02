package com.novelkeep.funding.domain;

public enum FundingCampaignStatus {

    DRAFT("미진행"),
    /** 모집·참여가 열려 있는 상태. 화면 표기는 「펀딩 중」. */
    IN_PROGRESS("펀딩 중"),
    SUCCESS("성공"),
    FAILED("실패"),
    CANCELLED("취소");

    private final String displayName;

    FundingCampaignStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
