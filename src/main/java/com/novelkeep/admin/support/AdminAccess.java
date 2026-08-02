package com.novelkeep.admin.support;

import com.novelkeep.home.domain.ExperienceRole;

public final class AdminAccess {

    public static final String SESSION_ROLE = "experienceRole";
    public static final String SESSION_MEMBER_ID = "memberId";

    private AdminAccess() {
    }

    public static boolean isOperator(ExperienceRole role, Long memberId) {
        return role == ExperienceRole.ADMIN && memberId != null;
    }
}
