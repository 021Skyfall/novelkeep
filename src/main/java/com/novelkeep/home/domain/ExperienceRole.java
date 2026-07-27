package com.novelkeep.home.domain;

import java.util.Arrays;
import java.util.Optional;

import com.novelkeep.member.domain.MemberType;

public enum ExperienceRole {

    READER("reader", MemberType.READER),
    WRITER("writer", MemberType.AUTHOR),
    ADMIN("admin", MemberType.ADMIN);

    private final String path;
    private final MemberType memberType;

    ExperienceRole(String path, MemberType memberType) {
        this.path = path;
        this.memberType = memberType;
    }

    public String getDisplayName() {
        return memberType.getDisplayName();
    }

    public MemberType getMemberType() {
        return memberType;
    }

    public static Optional<ExperienceRole> fromPath(String path) {
        return Arrays.stream(values())
                .filter(role -> role.path.equalsIgnoreCase(path))
                .findFirst();
    }

    public static Optional<ExperienceRole> fromMemberType(MemberType memberType) {
        return Arrays.stream(values())
                .filter(role -> role.memberType == memberType)
                .findFirst();
    }
}
