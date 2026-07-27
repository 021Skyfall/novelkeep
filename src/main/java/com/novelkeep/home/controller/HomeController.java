package com.novelkeep.home.controller;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.member.domain.Member;
import com.novelkeep.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final MemberService memberService;

    public HomeController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @PostMapping("/experience/{role}")
    public String selectRole(
            @PathVariable String role,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return ExperienceRole.fromPath(role)
                .map(selectedRole -> {
                    Member member = memberService.getOrCreateExperienceMember(selectedRole.getMemberType());
                    session.setAttribute(SESSION_ROLE, selectedRole);
                    session.setAttribute(SESSION_MEMBER_ID, member.getId());
                    return "redirect:/main";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("roleError", "선택할 수 없는 체험 역할입니다.");
                    return "redirect:/";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    @GetMapping("/main")
    public String main(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        ExperienceRole role = session == null
                ? null
                : (ExperienceRole) session.getAttribute(SESSION_ROLE);
        Long memberId = session == null
                ? null
                : (Long) session.getAttribute(SESSION_MEMBER_ID);

        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }

        model.addAttribute("roleKey", role.name());
        model.addAttribute("roleName", role.getDisplayName());
        return "main";
    }
}
