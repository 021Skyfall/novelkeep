package com.novelkeep.novel.dto;

public class EpisodeCommentResult {

    private final boolean ok;
    private final String action;
    private final String message;
    private final Long episodeId;

    public EpisodeCommentResult(boolean ok, String action, String message, Long episodeId) {
        this.ok = ok;
        this.action = action;
        this.message = message;
        this.episodeId = episodeId;
    }

    public static EpisodeCommentResult success(String action, String message, Long episodeId) {
        return new EpisodeCommentResult(true, action, message, episodeId);
    }

    public static EpisodeCommentResult failure(String message, Long episodeId) {
        return new EpisodeCommentResult(false, "error", message, episodeId);
    }

    public boolean isOk() {
        return ok;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public Long getEpisodeId() {
        return episodeId;
    }
}
