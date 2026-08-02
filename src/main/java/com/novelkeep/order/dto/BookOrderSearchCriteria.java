package com.novelkeep.order.dto;

import java.time.LocalDate;

import com.novelkeep.order.domain.BookOrderStatus;

public class BookOrderSearchCriteria {

    public enum SortField {
        ID("주문번호"),
        ORDERED("주문일"),
        STATUS("상태");

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
    private BookOrderStatus status;
    private LocalDate orderedFrom;
    private LocalDate orderedTo;
    private SortField sortField;
    private SortDir sortDir;

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public BookOrderStatus getStatus() {
        return status;
    }

    public void setStatus(BookOrderStatus status) {
        this.status = status;
    }

    public LocalDate getOrderedFrom() {
        return orderedFrom;
    }

    public void setOrderedFrom(LocalDate orderedFrom) {
        this.orderedFrom = orderedFrom;
    }

    public LocalDate getOrderedTo() {
        return orderedTo;
    }

    public void setOrderedTo(LocalDate orderedTo) {
        this.orderedTo = orderedTo;
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

    public String normalizedTitle() {
        if (novelTitle == null || novelTitle.isBlank()) {
            return null;
        }
        return novelTitle.trim();
    }
}
