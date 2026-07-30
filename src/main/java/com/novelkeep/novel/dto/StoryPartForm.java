package com.novelkeep.novel.dto;

import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StoryPartForm {

    @NotBlank(message = "부 제목을 입력해 주세요.")
    @Size(max = 100, message = "부 제목은 100자 이내로 입력해 주세요.")
    private String title;

    @NotNull(message = "부 상태를 선택해 주세요.")
    private StoryPartStatus status = StoryPartStatus.SERIALIZING;

    public static StoryPartForm from(StoryPart part) {
        StoryPartForm form = new StoryPartForm();
        form.title = part.getTitle();
        form.status = part.getStatus();
        return form;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public StoryPartStatus getStatus() {
        return status;
    }

    public void setStatus(StoryPartStatus status) {
        this.status = status;
    }
}
