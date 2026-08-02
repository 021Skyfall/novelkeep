package com.novelkeep.funding.controller;

import com.novelkeep.funding.dto.ReaderActivitySearchCriteria;
import com.novelkeep.funding.service.ReaderActivityService;
import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.order.domain.BookOrderStatus;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class ReaderActivityController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final ReaderActivityService readerActivityService;

    public ReaderActivityController(ReaderActivityService readerActivityService) {
        this.readerActivityService = readerActivityService;
    }

    @GetMapping("/reader/activities")
    public Object activities(
            @ModelAttribute("criteria") ReaderActivitySearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            Model model
    ) {
        if (role == null || memberId == null || role == ExperienceRole.ADMIN) {
            if (wantsFragment(request)) {
                return ResponseEntity.status(401).build();
            }
            return "redirect:/?roleRequired=true";
        }
        applyEntrySortDefaults(criteria, request);
        model.addAttribute("activities", readerActivityService.findActivities(memberId, criteria));
        model.addAttribute("tabOptions", ReaderActivitySearchCriteria.Tab.values());
        model.addAttribute("orderStatusOptions", BookOrderStatus.values());
        model.addAttribute("sortFields", ReaderActivitySearchCriteria.SortField.values());
        model.addAttribute("navActive", "activities");
        if (wantsFragment(request)) {
            return "reader/activities :: activityList";
        }
        return "reader/activities";
    }

    private void applyEntrySortDefaults(ReaderActivitySearchCriteria criteria, HttpServletRequest request) {
        if ("true".equalsIgnoreCase(request.getParameter("unsorted"))) {
            criteria.setSortField(null);
            criteria.setSortDir(null);
            return;
        }
        if (!request.getParameterMap().containsKey("sortField")) {
            criteria.setSortField(ReaderActivitySearchCriteria.SortField.RECENT);
            criteria.setSortDir(ReaderActivitySearchCriteria.SortDir.DESC);
        }
    }

    private boolean wantsFragment(HttpServletRequest request) {
        if ("1".equals(request.getParameter("partial")) || "true".equalsIgnoreCase(request.getParameter("partial"))) {
            return true;
        }
        return "1".equals(request.getHeader("X-Partial"));
    }
}
