package com.novelkeep.admin.controller;

import com.novelkeep.admin.service.AdminDashboardService;
import com.novelkeep.home.domain.ExperienceRole;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping({"", "/"})
    public String dashboard(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("stats", adminDashboardService.load());
        model.addAttribute("navActive", "admin-dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!isOperator(role, memberId)) {
            return ResponseEntity.status(401).build();
        }
        byte[] body = adminDashboardService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("novelkeep-dashboard.csv")
                        .build()
                        .toString())
                .contentType(new MediaType("text", "csv"))
                .body(body);
    }

    @GetMapping("/export.json")
    public ResponseEntity<byte[]> exportJson(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!isOperator(role, memberId)) {
            return ResponseEntity.status(401).build();
        }
        byte[] body = adminDashboardService.exportJson();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("novelkeep-dashboard.json")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private boolean isOperator(ExperienceRole role, Long memberId) {
        return role == ExperienceRole.ADMIN && memberId != null;
    }
}
