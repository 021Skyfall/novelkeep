package com.novelkeep.novel.dto;

import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EpisodeForm {

    @NotBlank(message = "회차 제목을 입력해 주세요.")
    @Size(max = 150, message = "회차 제목은 150자 이내로 입력해 주세요.")
    private String title;

    @NotBlank(message = "회차 본문을 입력해 주세요.")
    @Size(max = 200000, message = "회차 본문은 200,000자 이내로 입력해 주세요.")
    private String content;

    @NotNull(message = "회차 상태를 선택해 주세요.")
    private EpisodeStatus status = EpisodeStatus.UNPUBLISHED;

    public static EpisodeForm from(Episode episode) {
        EpisodeForm form = new EpisodeForm();
        form.title = episode.getTitle();
        form.content = episode.getContent();
        form.status = episode.getStatus();
        return form;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public void setStatus(EpisodeStatus status) {
        this.status = status;
    }
}
