package com.novelkeep.novel.domain;

public enum StoryPartStatus {

    DRAFT("초안"),
    SERIALIZING("연재 중"),
    COMPLETED("완결");

    private final String displayName;

    StoryPartStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
