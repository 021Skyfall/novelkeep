package com.novelkeep.novel.dto;

public class EpisodeBookmarkResult {

    private final boolean bookmarked;
    private final Long episodeId;
    private final Integer episodeNumber;
    private final String message;

    private EpisodeBookmarkResult(boolean bookmarked, Long episodeId, Integer episodeNumber, String message) {
        this.bookmarked = bookmarked;
        this.episodeId = episodeId;
        this.episodeNumber = episodeNumber;
        this.message = message;
    }

    public static EpisodeBookmarkResult saved(Long episodeId, int episodeNumber) {
        return new EpisodeBookmarkResult(
                true,
                episodeId,
                episodeNumber,
                episodeNumber + "화가 책갈피로 저장되었습니다. 내 책갈피에서 이어 읽을 수 있습니다."
        );
    }

    public static EpisodeBookmarkResult removed() {
        return new EpisodeBookmarkResult(false, null, null, "현재 작품의 책갈피가 해제되었습니다.");
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public String getMessage() {
        return message;
    }
}
