package com.novelkeep.funding.dto;

import com.novelkeep.funding.domain.FundingCampaignStatus;

public class AdminFundingSearchCriteria {

    public enum ApprovalFilter {
        ALL("전체"),
        AWAITING("승인 대기"),
        APPROVED("승인 완료");

        private final String displayName;

        ApprovalFilter(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum SortField {
        CLOSED("요청일"),
        GAUGE("달성률"),
        PRICE("판매가"),
        TARGET("목표 부수"),
        END("종료일");

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
    private ApprovalFilter approval = ApprovalFilter.AWAITING;
    private FundingCampaignStatus status;
    private SortField sortField;
    private SortDir sortDir;

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public ApprovalFilter getApproval() {
        return approval == null ? ApprovalFilter.AWAITING : approval;
    }

    public void setApproval(ApprovalFilter approval) {
        this.approval = approval;
    }

    public FundingCampaignStatus getStatus() {
        return status;
    }

    public void setStatus(FundingCampaignStatus status) {
        this.status = status;
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
