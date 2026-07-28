package com.novelkeep.novel.domain;

public enum NovelStatus {

    DRAFT("초안"),
    SERIALIZING("연재 중"),
    COMPLETED("전체 완결");

    private final String displayName;

    NovelStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
