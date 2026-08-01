package com.novelkeep.funding.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.dto.FundingActionResult;
import com.novelkeep.funding.service.FundingCampaignService;
import com.novelkeep.home.domain.ExperienceRole;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        boolean ownCampaign = campaign.getStoryPart().getNovel().getAuthor().getId().equals(memberId);
        boolean alreadyParticipated = fundingCampaignService.hasParticipated(campaignId, memberId);
        boolean openForJoin = campaign.isOpenForJoin(FundingGuide.nowKorea());
        boolean canParticipate = openForJoin && !ownCampaign && !alreadyParticipated;

        model.addAttribute("campaign", campaign);
        model.addAttribute("novel", campaign.getStoryPart().getNovel());
        model.addAttribute("part", campaign.getStoryPart());
        model.addAttribute("ownCampaign", ownCampaign);
        model.addAttribute("alreadyParticipated", alreadyParticipated);
        model.addAttribute("openForJoin", openForJoin);
        model.addAttribute("canParticipate", canParticipate);
        model.addAttribute("participateHint", resolveParticipateHint(
                campaign, ownCampaign, alreadyParticipated, openForJoin
        ));
        return "fundings/detail";
    }

    @PostMapping("/fundings/{campaignId}/participate")
    public Object participate(
            @PathVariable Long campaignId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (role == null || memberId == null) {
            if (wantsJson(request)) {
                return ResponseEntity.status(401).body(FundingActionResult.fail("체험 역할을 선택해 주세요."));
            }
            return "redirect:/?roleRequired=true";
        }
        try {
            FundingCampaign campaign = fundingCampaignService.participate(campaignId, memberId);
            String message = "모의 결제 " + formatWon(campaign.getPriceAmount()) + "으로 1부 참여했습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(toResult(message, campaign));
            }
            redirectAttributes.addFlashAttribute("fundingMessage", message);
            return "redirect:/fundings/" + campaignId;
        } catch (ResponseStatusException ex) {
            String message = resolveMessage(ex);
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(message));
            }
            redirectAttributes.addFlashAttribute("fundingError", message);
            return "redirect:/fundings/" + campaignId;
        }
    }

    private String resolveParticipateHint(
            FundingCampaign campaign,
            boolean ownCampaign,
            boolean alreadyParticipated,
            boolean openForJoin
    ) {
        if (alreadyParticipated) {
            return "이미 이 펀딩에 참여했습니다. 모의 결제 완료 상태입니다.";
        }
        if (ownCampaign) {
            return "본인 작품의 펀딩에는 참여할 수 없습니다.";
        }
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            return "이 펀딩은 진행 중이 아닙니다.";
        }
        if (!openForJoin) {
            return "펀딩 기간이 아니어서 지금은 참여할 수 없습니다.";
        }
        return "참여 수량은 1부로 고정됩니다. 같은 펀딩에는 한 번만 참여할 수 있습니다. "
                + "부 완결과 목표 부수 달성 시 출판 전환됩니다. 분량은 안내만 합니다.";
    }

    private FundingActionResult toResult(String message, FundingCampaign campaign) {
        return FundingActionResult.ok(
                message,
                campaign.getId(),
                campaign.getStoryPart().getId(),
                campaign.getStoryPart().getNovel().getId(),
                campaign.achievementPercent(),
                campaign.getCurrentQuantity(),
                campaign.getTargetQuantity()
        );
    }

    private boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private String resolveMessage(ResponseStatusException ex) {
        String reason = ex.getReason();
        return reason == null || reason.isBlank() ? "요청을 처리할 수 없습니다." : reason;
    }

    private String formatWon(BigDecimal amount) {
        if (amount == null) {
            return "0원";
        }
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(amount) + "원";
    }
}
