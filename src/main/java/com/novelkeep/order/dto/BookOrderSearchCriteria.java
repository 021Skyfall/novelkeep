package com.novelkeep.order.dto;

import java.time.LocalDate;

import com.novelkeep.order.domain.BookOrderStatus;

public class BookOrderSearchCriteria {

    private String novelTitle;
    private BookOrderStatus status;
    private LocalDate orderedFrom;
    private LocalDate orderedTo;

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

    public String normalizedTitle() {
        if (novelTitle == null || novelTitle.isBlank()) {
            return null;
        }
        return novelTitle.trim();
    }
}
