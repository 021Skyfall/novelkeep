package com.novelkeep.order.domain;

public enum BookOrderStatus {

    PENDING("접수"),
    PROCESSING("제작 중"),
    COMPLETED("제작 완료");

    private final String displayName;

    BookOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BookOrderStatus next() {
        return switch (this) {
            case PENDING -> PROCESSING;
            case PROCESSING -> COMPLETED;
            case COMPLETED -> null;
        };
    }
}
