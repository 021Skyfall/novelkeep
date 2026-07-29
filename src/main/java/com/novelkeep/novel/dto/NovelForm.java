package com.novelkeep.novel.dto;

import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.PartMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NovelForm {

    @NotBlank(message = "작품 제목을 입력해 주세요.")
    @Size(max = 100, message = "작품 제목은 100자 이내로 입력해 주세요.")
    private String title;

    @NotBlank(message = "공개 필명을 입력해 주세요.")
    @Size(max = 50, message = "공개 필명은 50자 이내로 입력해 주세요.")
    private String penName;

    @NotBlank(message = "장르를 입력해 주세요.")
    @Size(max = 30, message = "장르는 30자 이내로 입력해 주세요.")
    private String genre;

    @NotBlank(message = "작품 소개를 입력해 주세요.")
    @Size(max = 2000, message = "작품 소개는 2,000자 이내로 입력해 주세요.")
    private String synopsis;

    @NotNull(message = "작품 상태를 선택해 주세요.")
    private NovelStatus status = NovelStatus.SERIALIZING;

    @NotNull(message = "공개 여부를 선택해 주세요.")
    private NovelVisibility visibility = NovelVisibility.PRIVATE;

    @NotNull(message = "부 구분 사용 여부를 선택해 주세요.")
    private PartMode partMode = PartMode.SINGLE;

    @Size(max = 100, message = "첫 부 제목은 100자 이내로 입력해 주세요.")
    private String firstPartTitle;

    public static NovelForm from(Novel novel) {
        NovelForm form = new NovelForm();
        form.title = novel.getTitle();
        form.penName = novel.getPenName();
        form.genre = novel.getGenre();
        form.synopsis = novel.getSynopsis();
        form.status = novel.getStatus();
        form.visibility = novel.getVisibility();
        form.partMode = novel.getPartMode();
        form.firstPartTitle = novel.getParts().isEmpty() ? null : novel.getParts().getFirst().getTitle();
        return form;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPenName() {
        return penName;
    }

    public void setPenName(String penName) {
        this.penName = penName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public NovelStatus getStatus() {
        return status;
    }

    public void setStatus(NovelStatus status) {
        this.status = status;
    }

    public NovelVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(NovelVisibility visibility) {
        this.visibility = visibility;
    }

    public PartMode getPartMode() {
        return partMode;
    }

    public void setPartMode(PartMode partMode) {
        this.partMode = partMode;
    }

    public String getFirstPartTitle() {
        return firstPartTitle;
    }

    public void setFirstPartTitle(String firstPartTitle) {
        this.firstPartTitle = firstPartTitle;
    }
}
