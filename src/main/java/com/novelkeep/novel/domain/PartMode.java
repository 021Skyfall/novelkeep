package com.novelkeep.novel.domain;

public enum PartMode {

    SINGLE("부 구분 없음"),
    MULTI("부 구분 사용");

    private final String displayName;

    PartMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
