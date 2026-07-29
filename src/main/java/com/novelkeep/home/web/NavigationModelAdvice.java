package com.novelkeep.home.web;

import com.novelkeep.home.domain.ExperienceRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavigationModelAdvice {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    @ModelAttribute("roleKey")
    public String roleKey(HttpServletRequest request) {
        ExperienceRole role = currentRole(request);
        return role == null ? null : role.name();
    }

    @ModelAttribute("roleName")
    public String roleName(HttpServletRequest request) {
        ExperienceRole role = currentRole(request);
        return role == null ? null : role.getDisplayName();
    }

    @ModelAttribute("canWrite")
    public boolean canWrite(HttpServletRequest request) {
        ExperienceRole role = currentRole(request);
        return role == ExperienceRole.WRITER || role == ExperienceRole.ADMIN;
    }

    @ModelAttribute("memberId")
    public Long memberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_MEMBER_ID);
        return value instanceof Long id ? id : null;
    }

    private ExperienceRole currentRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_ROLE);
        return value instanceof ExperienceRole role ? role : null;
    }
}
