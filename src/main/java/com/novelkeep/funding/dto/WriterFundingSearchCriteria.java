package com.novelkeep.funding.dto;

import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.order.domain.BookOrderStatus;

public class WriterFundingSearchCriteria {

    public enum SortField {
        UPDATED("최근 수정"),
        END("종료일"),
        GAUGE("달성률"),
        TARGET("목표 부수"),
        PRICE("판매가");

        private final String displayName;

        SortField(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum SortDir {
        ASC,
        DESC
    }

    private String novelTitle;
    private FundingCampaignStatus status;
    private BookOrderStatus orderStatus;
    private SortField sortField;
    private SortDir sortDir;

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public FundingCampaignStatus getStatus() {
        return status;
    }

    public void setStatus(FundingCampaignStatus status) {
        this.status = status;
    }

    public BookOrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(BookOrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public SortField getSortField() {
        return sortField;
    }

    public void setSortField(SortField sortField) {
        this.sortField = sortField;
    }

    public SortDir getSortDir() {
        return sortDir;
    }

    public void setSortDir(SortDir sortDir) {
        this.sortDir = sortDir;
    }
}
