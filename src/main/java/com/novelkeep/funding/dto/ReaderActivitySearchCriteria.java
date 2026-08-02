package com.novelkeep.funding.dto;

import com.novelkeep.order.domain.BookOrderStatus;

public class ReaderActivitySearchCriteria {

    public enum Tab {
        ALL("전체"),
        ACTIVE("펀딩 중"),
        ORDER("주문"),
        REFUND("환불");

        private final String displayName;

        Tab(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ActivityStatus {
        PARTICIPATING("참여"),
        REFUNDED("환불 완료");

        private final String displayName;

        ActivityStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum SortField {
        RECENT("최근"),
        END("종료일"),
        GAUGE("달성률");

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

    private Tab tab = Tab.ALL;
    private String novelTitle;
    private BookOrderStatus orderStatus;
    private ActivityStatus activityStatus;
    private SortField sortField;
    private SortDir sortDir;

    public Tab getTab() {
        return tab == null ? Tab.ALL : tab;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public BookOrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(BookOrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public ActivityStatus getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(ActivityStatus activityStatus) {
        this.activityStatus = activityStatus;
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
