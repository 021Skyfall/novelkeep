package com.novelkeep.funding.domain;

public enum FundingPaymentStatus {

    PAID_MOCK("결제 완료"),
    REFUNDED_MOCK("환불 완료");

    private final String displayName;

    FundingPaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
