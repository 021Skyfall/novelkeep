package com.novelkeep.novel.domain;

public enum EpisodeStatus {

    UNPUBLISHED("비공개"),
    PUBLISHED("공개");

    private final String displayName;

    EpisodeStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
