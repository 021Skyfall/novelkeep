package com.novelkeep.novel.dto;

public class StoryContentActionResult {

    private final boolean ok;
    private final String action;
    private final String message;
    private final Long novelId;

    public StoryContentActionResult(boolean ok, String action, String message, Long novelId) {
        this.ok = ok;
        this.action = action;
        this.message = message;
        this.novelId = novelId;
    }

    public static StoryContentActionResult success(String action, String message, Long novelId) {
        return new StoryContentActionResult(true, action, message, novelId);
    }

    public static StoryContentActionResult failure(String message, Long novelId) {
        return new StoryContentActionResult(false, "error", message, novelId);
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

    public Long getNovelId() {
        return novelId;
    }
}
