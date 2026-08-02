package com.novelkeep.admin.controller;

import com.novelkeep.admin.service.AdminDashboardService;
import com.novelkeep.admin.support.AdminAccess;
import com.novelkeep.admin.support.AdminExports;
import com.novelkeep.home.domain.ExperienceRole;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping({"", "/"})
    public String dashboard(
            @SessionAttribute(name = AdminAccess.SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = AdminAccess.SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!AdminAccess.isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("stats", adminDashboardService.load());
        model.addAttribute("navActive", "admin-dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @SessionAttribute(name = AdminAccess.SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = AdminAccess.SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!AdminAccess.isOperator(role, memberId)) {
            return ResponseEntity.status(401).build();
        }
        return AdminExports.csv("novelkeep-dashboard.csv", adminDashboardService.exportCsv());
    }

    @GetMapping("/export.json")
    public ResponseEntity<byte[]> exportJson(
            @SessionAttribute(name = AdminAccess.SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = AdminAccess.SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!AdminAccess.isOperator(role, memberId)) {
            return ResponseEntity.status(401).build();
        }
        return AdminExports.json("novelkeep-dashboard.json", adminDashboardService.exportJson());
    }
}
