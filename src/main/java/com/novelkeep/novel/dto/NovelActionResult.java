package com.novelkeep.novel.dto;

public class NovelActionResult {

    private final boolean active;
    private final long recommendationCount;

    public NovelActionResult(boolean active, long recommendationCount) {
        this.active = active;
        this.recommendationCount = recommendationCount;
    }

    public static NovelActionResult of(boolean active, long recommendationCount) {
        return new NovelActionResult(active, recommendationCount);
    }

    public static NovelActionResult favorite(boolean active) {
        return new NovelActionResult(active, 0L);
    }

    public boolean isActive() {
        return active;
    }

    public long getRecommendationCount() {
        return recommendationCount;
    }
}
