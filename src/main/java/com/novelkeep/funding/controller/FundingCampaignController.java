package com.novelkeep.funding.controller;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.service.FundingCampaignService;
import com.novelkeep.home.domain.ExperienceRole;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class FundingCampaignController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final FundingCampaignService fundingCampaignService;

    public FundingCampaignController(FundingCampaignService fundingCampaignService) {
        this.fundingCampaignService = fundingCampaignService;
    }

    @GetMapping("/fundings/{campaignId}")
    public String detail(
            @PathVariable Long campaignId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        FundingCampaign campaign = fundingCampaignService.findReadable(campaignId);
        model.addAttribute("campaign", campaign);
        model.addAttribute("novel", campaign.getStoryPart().getNovel());
        model.addAttribute("part", campaign.getStoryPart());
        return "fundings/detail";
    }
}
