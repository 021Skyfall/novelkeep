package com.novelkeep.funding.controller;

import java.util.List;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.dto.FundingActionResult;
import com.novelkeep.funding.dto.WriterFundingForm;
import com.novelkeep.funding.service.FundingCampaignService;
import com.novelkeep.home.domain.ExperienceRole;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WriterPublishingController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final FundingCampaignService fundingCampaignService;

    public WriterPublishingController(FundingCampaignService fundingCampaignService) {
        this.fundingCampaignService = fundingCampaignService;
    }

    @GetMapping({"/writer/publishing", "/writer/novels/{novelId}/publishing"})
    public String publishing(
            @PathVariable(required = false) Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }

        List<FundingCampaign> campaigns = fundingCampaignService.findOpenOwnedCampaigns(memberId);
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("minTargetQuantity", FundingGuide.MIN_TARGET_QUANTITY);
        model.addAttribute("minDurationDays", FundingGuide.MIN_DURATION_DAYS);
        model.addAttribute("guideVolumeChars", FundingGuide.GUIDE_VOLUME_CHARS);
        return "writer/publishing";
    }

    @PostMapping("/writer/publishing/campaigns")
    public Object startCampaign(
            @Valid @ModelAttribute("fundingForm") WriterFundingForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        if (bindingResult.hasErrors()) {
            String message = "펀딩 입력을 확인해 주세요. 목표 부수는 "
                    + FundingGuide.MIN_TARGET_QUANTITY + "부 이상, 기간은 최소 "
                    + FundingGuide.MIN_DURATION_DAYS + "일입니다.";
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(message));
            }
            redirectAttributes.addFlashAttribute("publishingError", message);
            return redirectAfterCreateFailure(form.getNovelId());
        }
        try {
            FundingCampaign campaign = fundingCampaignService.startCampaign(memberId, form);
            if (wantsJson(request)) {
                return ResponseEntity.ok(toResult("펀딩을 시작했습니다.", campaign));
            }
            redirectAttributes.addFlashAttribute("publishingMessage", "펀딩을 시작했습니다.");
            return "redirect:/novels/" + form.getNovelId() + "?from=writer";
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(resolveMessage(ex)));
            }
            redirectAttributes.addFlashAttribute("publishingError", resolveMessage(ex));
            return redirectAfterCreateFailure(form.getNovelId());
        }
    }

    @PostMapping("/writer/publishing/campaigns/{campaignId}")
    public Object updateCampaign(
            @PathVariable Long campaignId,
            @Valid @ModelAttribute("fundingForm") WriterFundingForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        if (bindingResult.hasErrors()) {
            String message = "펀딩 수정을 확인해 주세요.";
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(message));
            }
            redirectAttributes.addFlashAttribute("publishingError", message);
            return "redirect:/writer/publishing";
        }
        try {
            FundingCampaign campaign = fundingCampaignService.updateOpenCampaign(campaignId, memberId, form);
            if (wantsJson(request)) {
                return ResponseEntity.ok(toResult("펀딩을 수정했습니다.", campaign));
            }
            redirectAttributes.addFlashAttribute("publishingMessage", "펀딩을 수정했습니다.");
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(resolveMessage(ex)));
            }
            redirectAttributes.addFlashAttribute("publishingError", resolveMessage(ex));
        }
        return "redirect:/writer/publishing";
    }

    @PostMapping("/writer/publishing/campaigns/{campaignId}/open")
    public String openDraft(
            @PathVariable Long campaignId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        try {
            fundingCampaignService.openDraft(campaignId, memberId);
            redirectAttributes.addFlashAttribute("publishingMessage", "펀딩을 시작했습니다.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("publishingError", resolveMessage(ex));
        }
        return "redirect:/writer/publishing";
    }

    @PostMapping("/writer/publishing/campaigns/{campaignId}/cancel")
    public Object cancel(
            @PathVariable Long campaignId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        try {
            fundingCampaignService.cancel(campaignId, memberId);
            if (wantsJson(request)) {
                return ResponseEntity.ok(FundingActionResult.ok(
                        "펀딩을 취소했습니다.", campaignId, null, null, 0, 0, 0
                ));
            }
            redirectAttributes.addFlashAttribute("publishingMessage", "펀딩을 취소했습니다.");
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(resolveMessage(ex)));
            }
            redirectAttributes.addFlashAttribute("publishingError", resolveMessage(ex));
        }
        return "redirect:/writer/publishing";
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

    private String redirectAfterCreateFailure(Long novelId) {
        if (novelId != null) {
            return "redirect:/novels/" + novelId + "?from=writer";
        }
        return "redirect:/writer/novels";
    }

    private Object unauthorized(HttpServletRequest request) {
        if (wantsJson(request)) {
            return ResponseEntity.status(401).body(FundingActionResult.fail("작가로 체험해 주세요."));
        }
        return "redirect:/?roleRequired=true";
    }

    private boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private boolean canWrite(ExperienceRole role) {
        return role == ExperienceRole.WRITER;
    }

    private String resolveMessage(ResponseStatusException ex) {
        String reason = ex.getReason();
        return reason == null || reason.isBlank() ? "요청을 처리할 수 없습니다." : reason;
    }
}
