package com.novelkeep.home.web;

import com.novelkeep.home.domain.ExperienceRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OperatorAccessInterceptor implements HandlerInterceptor {

    private static final String SESSION_ROLE = "experienceRole";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return true;
        }
        Object value = session.getAttribute(SESSION_ROLE);
        if (!(value instanceof ExperienceRole role) || role != ExperienceRole.ADMIN) {
            return true;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }

        if (isAllowed(path)) {
            return true;
        }
        response.sendRedirect(contextPath + "/admin/orders");
        return false;
    }

    private boolean isAllowed(String path) {
        if ("/".equals(path)) {
            return true;
        }
        if (path.startsWith("/admin")) {
            return true;
        }
        if (path.startsWith("/fundings/") || path.startsWith("/novels/")) {
            return true;
        }
        if (path.startsWith("/experience") || path.equals("/logout") || path.startsWith("/logout")) {
            return true;
        }
        if (path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/vendor/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico")
                || path.equals("/error")) {
            return true;
        }
        return false;
    }
}
