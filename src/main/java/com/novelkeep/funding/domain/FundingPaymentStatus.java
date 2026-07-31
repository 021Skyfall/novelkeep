package com.novelkeep.funding.domain;

public enum FundingPaymentStatus {

    PAID_MOCK("모의 결제 완료"),
    REFUNDED_MOCK("모의 환불");

    private final String displayName;

    FundingPaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
