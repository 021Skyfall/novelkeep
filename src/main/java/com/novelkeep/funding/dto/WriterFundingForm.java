package com.novelkeep.funding.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingGuide;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

public class WriterFundingForm {

    @NotNull(message = "작품을 선택해 주세요.")
    private Long novelId;

    @NotNull(message = "대상 부를 선택해 주세요.")
    private Long partId;

    @NotNull(message = "목표 부수를 입력해 주세요.")
    @Min(value = 10, message = "목표 부수는 10부 이상이어야 합니다.")
    private Integer targetQuantity = 50;

    @NotNull(message = "판매가를 입력해 주세요.")
    @DecimalMin(value = "1", message = "판매가는 1 이상이어야 합니다.")
    private BigDecimal priceAmount = BigDecimal.valueOf(15000);

    @NotNull(message = "시작 시각을 입력해 주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "종료 시각을 입력해 주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    public static WriterFundingForm defaults() {
        WriterFundingForm form = new WriterFundingForm();
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        form.startAt = now;
        form.endAt = now.plusDays(FundingGuide.MIN_DURATION_DAYS);
        form.targetQuantity = FundingGuide.MIN_TARGET_QUANTITY;
        return form;
    }

    public static WriterFundingForm from(FundingCampaign campaign) {
        WriterFundingForm form = new WriterFundingForm();
        form.novelId = campaign.getStoryPart().getNovel().getId();
        form.partId = campaign.getStoryPart().getId();
        form.targetQuantity = campaign.getTargetQuantity();
        form.priceAmount = campaign.getPriceAmount();
        form.startAt = campaign.getStartAt();
        form.endAt = campaign.getEndAt();
        return form;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public Integer getTargetQuantity() {
        return targetQuantity;
    }

    public void setTargetQuantity(Integer targetQuantity) {
        this.targetQuantity = targetQuantity;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }
}
