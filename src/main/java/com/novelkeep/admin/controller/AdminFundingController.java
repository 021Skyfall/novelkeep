package com.novelkeep.admin.controller;

import java.util.List;

import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.dto.AdminFundingSearchCriteria;
import com.novelkeep.funding.dto.FundingApproveResult;
import com.novelkeep.funding.service.FundingCampaignService;
import com.novelkeep.home.domain.ExperienceRole;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin")
public class AdminFundingController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final FundingCampaignService fundingCampaignService;

    public AdminFundingController(FundingCampaignService fundingCampaignService) {
        this.fundingCampaignService = fundingCampaignService;
    }

    @GetMapping("/fundings")
    public String fundings(
            @ModelAttribute("criteria") AdminFundingSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            jakarta.servlet.http.HttpServletRequest request,
            Model model
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        applyEntrySortDefaults(criteria, request);
        model.addAttribute("campaigns", fundingCampaignService.findAdminCampaigns(criteria));
        model.addAttribute("criteria", criteria);
        model.addAttribute("approvalOptions", AdminFundingSearchCriteria.ApprovalFilter.values());
        model.addAttribute("statusOptions", List.of(
                FundingCampaignStatus.SUCCESS,
                FundingCampaignStatus.FAILED
        ));
        model.addAttribute("sortFields", AdminFundingSearchCriteria.SortField.values());
        model.addAttribute("navActive", "admin-fundings");
        if (wantsFragment(request)) {
            return "admin/fundings :: fundingList";
        }
        return "admin/fundings";
    }

    private void applyEntrySortDefaults(AdminFundingSearchCriteria criteria, jakarta.servlet.http.HttpServletRequest request) {
        if ("true".equalsIgnoreCase(request.getParameter("unsorted"))) {
            criteria.setSortField(null);
            criteria.setSortDir(null);
            return;
        }
        if (!request.getParameterMap().containsKey("sortField")) {
            criteria.setSortField(AdminFundingSearchCriteria.SortField.CLOSED);
            criteria.setSortDir(AdminFundingSearchCriteria.SortDir.DESC);
        }
    }

    @PostMapping("/fundings/{campaignId}/approve")
    public String approve(
            @PathVariable Long campaignId,
            @ModelAttribute AdminFundingSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        try {
            FundingApproveResult result = fundingCampaignService.approveCampaign(campaignId);
            String message = result.success()
                    ? "성공 펀딩을 승인했습니다. 주문 " + result.totalQuantity() + "건이 접수되었습니다."
                    : "실패 펀딩을 승인했습니다. 참여 " + result.totalQuantity() + "건을 환불했습니다.";
            redirectAttributes.addFlashAttribute("fundingMessage", message);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute(
                    "fundingError",
                    ex.getReason() == null ? "승인할 수 없습니다." : ex.getReason()
            );
        }
        return redirectWithCriteria(criteria);
    }

    @PostMapping("/fundings/{campaignId}/reject")
    public String reject(
            @PathVariable Long campaignId,
            @ModelAttribute AdminFundingSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        try {
            fundingCampaignService.rejectCampaign(campaignId);
            redirectAttributes.addFlashAttribute("fundingMessage", "거절했습니다. 펀딩이 진행 중으로 돌아갔습니다.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute(
                    "fundingError",
                    ex.getReason() == null ? "거절할 수 없습니다." : ex.getReason()
            );
        }
        return redirectWithCriteria(criteria);
    }

    private String redirectWithCriteria(AdminFundingSearchCriteria criteria) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/fundings");
        if (criteria.getNovelTitle() != null && !criteria.getNovelTitle().isBlank()) {
            builder.queryParam("novelTitle", criteria.getNovelTitle().trim());
        }
        if (criteria.getApproval() != null) {
            builder.queryParam("approval", criteria.getApproval().name());
        }
        if (criteria.getStatus() != null) {
            builder.queryParam("status", criteria.getStatus().name());
        }
        if (criteria.getSortField() != null) {
            builder.queryParam("sortField", criteria.getSortField().name());
        }
        if (criteria.getSortDir() != null) {
            builder.queryParam("sortDir", criteria.getSortDir().name());
        }
        return "redirect:" + builder.build().encode().toUriString();
    }

    private boolean wantsFragment(jakarta.servlet.http.HttpServletRequest request) {
        return "1".equals(request.getHeader("X-Partial"))
                || "1".equals(request.getParameter("partial"));
    }

    private boolean isOperator(ExperienceRole role, Long memberId) {
        return role == ExperienceRole.ADMIN && memberId != null;
    }
}
