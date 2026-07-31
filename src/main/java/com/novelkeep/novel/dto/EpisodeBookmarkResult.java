package com.novelkeep.novel.dto;

public class EpisodeBookmarkResult {

    private final boolean bookmarked;
    private final Long episodeId;
    private final Integer episodeNumber;
    private final String novelTitle;
    private final String partLabel;
    private final String message;

    private EpisodeBookmarkResult(
            boolean bookmarked,
            Long episodeId,
            Integer episodeNumber,
            String novelTitle,
            String partLabel,
            String message
    ) {
        this.bookmarked = bookmarked;
        this.episodeId = episodeId;
        this.episodeNumber = episodeNumber;
        this.novelTitle = novelTitle;
        this.partLabel = partLabel;
        this.message = message;
    }

    public static EpisodeBookmarkResult saved(
            Long episodeId,
            int episodeNumber,
            String novelTitle,
            String partLabel
    ) {
        StringBuilder message = new StringBuilder("『").append(novelTitle).append("』 ");
        if (partLabel != null && !partLabel.isBlank()) {
            message.append(partLabel).append(' ');
        }
        message.append(episodeNumber)
                .append("화가 책갈피로 저장되었습니다. 내 책갈피에서 이어 읽을 수 있습니다.");
        return new EpisodeBookmarkResult(
                true,
                episodeId,
                episodeNumber,
                novelTitle,
                partLabel,
                message.toString()
        );
    }

    public static EpisodeBookmarkResult removed() {
        return new EpisodeBookmarkResult(
                false,
                null,
                null,
                null,
                null,
                "현재 작품의 책갈피가 해제되었습니다."
        );
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

    public String getNovelTitle() {
        return novelTitle;
    }

    public String getPartLabel() {
        return partLabel;
    }

    public String getMessage() {
        return message;
    }
}
