package com.novelkeep.order.domain;

public enum BookOrderStatus {

    PENDING("접수"),
    PROCESSING("제작중"),
    PRODUCTION_DONE("제작완료"),
    SHIP_READY("배송준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송완료");

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
            case PROCESSING -> PRODUCTION_DONE;
            case PRODUCTION_DONE -> SHIP_READY;
            case SHIP_READY -> SHIPPING;
            case SHIPPING -> DELIVERED;
            case DELIVERED -> null;
        };
    }
}
