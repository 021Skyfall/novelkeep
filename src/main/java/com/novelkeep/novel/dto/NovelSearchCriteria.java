package com.novelkeep.novel.dto;

import com.novelkeep.novel.domain.NovelGenre;
import com.novelkeep.novel.domain.NovelVisibility;

public class NovelSearchCriteria {

    public static final String PROGRESS_SERIALIZING = "SERIALIZING";
    public static final String PROGRESS_PART_COMPLETED = "PART_COMPLETED";
    public static final String PROGRESS_COMPLETED = "COMPLETED";

    private String keyword;
    private NovelGenre genre;
    private String progress;
    private NovelVisibility visibility;
    private Boolean favorite;
    private String sort = "latest";
    private int page = 0;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public NovelGenre getGenre() {
        return genre;
    }

    public void setGenre(NovelGenre genre) {
        this.genre = genre;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public NovelVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(NovelVisibility visibility) {
        this.visibility = visibility;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }

    public boolean isTitleSort() {
        return "title".equalsIgnoreCase(sort);
    }

    public boolean isRecommendSort() {
        return "recommend".equalsIgnoreCase(sort);
    }

    public boolean isFavoriteOnly() {
        return Boolean.TRUE.equals(favorite);
    }

    public boolean isSerializingProgress() {
        return PROGRESS_SERIALIZING.equalsIgnoreCase(progress);
    }

    public boolean isPartCompletedProgress() {
        return PROGRESS_PART_COMPLETED.equalsIgnoreCase(progress);
    }

    public boolean isCompletedProgress() {
        return PROGRESS_COMPLETED.equalsIgnoreCase(progress);
    }
}
