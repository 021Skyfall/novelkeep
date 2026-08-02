package com.novelkeep.novel.domain;

public enum NovelVisibility {

    PUBLIC("공개"),
    PRIVATE("비공개");

    private final String displayName;

    NovelVisibility(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
