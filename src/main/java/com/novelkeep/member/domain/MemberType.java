package com.novelkeep.member.domain;

public enum MemberType {

    READER("독자"),
    AUTHOR("작가"),
    ADMIN("관리자");

    private final String displayName;

    MemberType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
