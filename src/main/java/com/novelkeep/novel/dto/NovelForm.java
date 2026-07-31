package com.novelkeep.novel.dto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelGenre;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NovelForm {

    @NotBlank(message = "작품 제목을 입력해 주세요.")
    @Size(max = 100, message = "작품 제목은 100자 이내로 입력해 주세요.")
    private String title;

    @NotBlank(message = "공개 필명을 입력해 주세요.")
    @Size(max = 50, message = "공개 필명은 50자 이내로 입력해 주세요.")
    private String penName;

    @NotEmpty(message = "장르를 하나 이상 선택해 주세요.")
    @Size(max = NovelGenre.MAX_PER_NOVEL, message = "장르는 최대 8개까지 선택할 수 있습니다.")
    private List<NovelGenre> genres = new ArrayList<>(List.of(NovelGenre.FANTASY));

    @NotBlank(message = "작품 소개를 입력해 주세요.")
    @Size(max = 2000, message = "작품 소개는 2,000자 이내로 입력해 주세요.")
    private String synopsis;

    @NotNull(message = "작품 상태를 선택해 주세요.")
    private NovelStatus status = NovelStatus.SERIALIZING;

    @NotNull(message = "공개 여부를 선택해 주세요.")
    private NovelVisibility visibility = NovelVisibility.PRIVATE;

    public static NovelForm from(Novel novel) {
        NovelForm form = new NovelForm();
        form.title = novel.getTitle();
        form.penName = novel.getPenName();
        form.genres = new ArrayList<>(novel.getGenres());
        form.synopsis = novel.getSynopsis();
        form.status = novel.getStatus();
        form.visibility = novel.getVisibility();
        return form;
    }

    public List<NovelGenre> normalizedGenres() {
        LinkedHashSet<NovelGenre> unique = new LinkedHashSet<>();
        if (genres != null) {
            for (NovelGenre genre : genres) {
                if (genre != null) {
                    unique.add(genre);
                }
            }
        }
        return new ArrayList<>(unique);
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

    public List<NovelGenre> getGenres() {
        return genres;
    }

    public void setGenres(List<NovelGenre> genres) {
        this.genres = genres;
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

}
