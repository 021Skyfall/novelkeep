package com.novelkeep.funding.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.dto.FundingActionResult;
import com.novelkeep.funding.service.FundingCampaignService;
import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.order.domain.BookOrderStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FundingCampaignController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";
    private static final String SESSION_FUNDING_BACK_URL = "fundingBackUrl";
    private static final String DEFAULT_BACK_URL = "/main#funding";
    private static final Pattern FUNDING_DETAIL_PATH = Pattern.compile(".*/fundings/\\d+(/.*)?$");
    private static final Pattern NOVEL_DETAIL_PATH = Pattern.compile(".*/novels/\\d+(/.*)?$");

    private final FundingCampaignService fundingCampaignService;

    public FundingCampaignController(FundingCampaignService fundingCampaignService) {
        this.fundingCampaignService = fundingCampaignService;
    }

    @GetMapping("/fundings/{campaignId}")
    public String detail(
            @PathVariable Long campaignId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            HttpSession session,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        FundingCampaign campaign = fundingCampaignService.findReadable(campaignId);
        boolean ownCampaign = campaign.getStoryPart().getNovel().getAuthor().getId().equals(memberId);
        boolean alreadyParticipated = fundingCampaignService.hasParticipated(campaignId, memberId);
        boolean paidParticipation = fundingCampaignService.hasPaidParticipation(campaignId, memberId);
        boolean openForJoin = campaign.isOpenForJoin(FundingGuide.nowKorea());
        boolean canParticipate = openForJoin && !ownCampaign && !alreadyParticipated;
        boolean canRefund = openForJoin && paidParticipation && !ownCampaign;
        int myQuantity = fundingCampaignService.findPaidQuantity(campaignId, memberId);
        String backUrl = resolveFundingBackUrl(session, request);
        String backLabel = resolveFundingBackLabel(backUrl);
        String navActive = resolveNavActive(backUrl);

        model.addAttribute("campaign", campaign);
        model.addAttribute("novel", campaign.getStoryPart().getNovel());
        model.addAttribute("part", campaign.getStoryPart());
        model.addAttribute("ownCampaign", ownCampaign);
        model.addAttribute("alreadyParticipated", alreadyParticipated);
        model.addAttribute("openForJoin", openForJoin);
        model.addAttribute("canParticipate", canParticipate);
        model.addAttribute("canRefund", canRefund);
        model.addAttribute("myQuantity", myQuantity);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("backLabel", backLabel);
        model.addAttribute("navActive", navActive);
        var memberOrderStatus = fundingCampaignService.findMemberOrderStatus(campaignId, memberId);
        model.addAttribute("memberOrderStatus", memberOrderStatus);
        model.addAttribute("participateHint", resolveParticipateHint(
                campaign, ownCampaign, alreadyParticipated, paidParticipation, openForJoin, memberOrderStatus
        ));
        return "fundings/detail";
    }

    @PostMapping("/fundings/{campaignId}/refund")
    public Object refund(
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
            FundingCampaign campaign = fundingCampaignService.cancelParticipation(campaignId, memberId);
            String message = "환불이 완료되었습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(toResult(message, campaign));
            }
            redirectAttributes.addFlashAttribute("fundingMessage", message);
            return "redirect:/fundings/" + campaignId + "?refunded=1";
        } catch (ResponseStatusException ex) {
            String message = resolveMessage(ex);
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(FundingActionResult.fail(message));
            }
            redirectAttributes.addFlashAttribute("fundingError", message);
            return "redirect:/fundings/" + campaignId;
        }
    }

    @PostMapping("/fundings/{campaignId}/participate")
    public Object participate(
            @PathVariable Long campaignId,
            @RequestParam(defaultValue = "1") int quantity,
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
            FundingCampaign campaign = fundingCampaignService.participate(campaignId, memberId, quantity);
            String message = "결제 "
                    + formatWon(campaign.getPriceAmount().multiply(java.math.BigDecimal.valueOf(quantity)))
                    + "으로 " + quantity + "부 참여했습니다.";
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

    /**
     * 진입 Referer가 유효하면 세션에 저장하고, 환불·새로고침처럼 동일 상세 재진입 시에는 기존 값을 유지한다.
     */
    private String resolveFundingBackUrl(HttpSession session, HttpServletRequest request) {
        String candidate = sanitizeFundingBackUrl(request.getHeader("Referer"), request);
        if (candidate != null) {
            session.setAttribute(SESSION_FUNDING_BACK_URL, candidate);
            return candidate;
        }
        Object saved = session.getAttribute(SESSION_FUNDING_BACK_URL);
        if (saved instanceof String url && isSafeInternalPath(url)) {
            return url;
        }
        return DEFAULT_BACK_URL;
    }

    private String sanitizeFundingBackUrl(String referer, HttpServletRequest request) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return null;
            }
            String path = uri.getPath() == null ? "" : uri.getPath();
            String context = request.getContextPath() == null ? "" : request.getContextPath();
            if (!context.isBlank() && path.startsWith(context)) {
                path = path.substring(context.length());
                if (path.isBlank()) {
                    path = "/";
                }
            }
            if (!isSafeInternalPath(path) || FUNDING_DETAIL_PATH.matcher(path).matches()) {
                return null;
            }
            String query = uri.getRawQuery();
            return path + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isSafeInternalPath(String value) {
        if (value == null || value.isBlank() || !value.startsWith("/") || value.startsWith("//")) {
            return false;
        }
        if (value.contains("://") || value.contains("\\")) {
            return false;
        }
        return !value.startsWith("/experience") && !"/logout".equals(value);
    }

    private String resolveFundingBackLabel(String backUrl) {
        String path = backUrl == null ? "" : backUrl.split("\\?", 2)[0];
        if (path.contains("/reader/activities")) {
            return "← 내 펀딩·주문";
        }
        if (path.contains("/writer/publishing")) {
            return "← 내 펀딩 관리";
        }
        if (NOVEL_DETAIL_PATH.matcher(path).matches()) {
            return "← 작품 상세";
        }
        if (path.contains("/novels")) {
            return "← 작품 목록";
        }
        if (path.contains("/admin/fundings")) {
            return "← 펀딩 관리";
        }
        if (path.contains("/admin/orders")) {
            return "← 주문 관리";
        }
        if (path.contains("/main") || path.startsWith("/main#")) {
            return "← 메인";
        }
        return "← 이전 화면";
    }

    private String resolveNavActive(String backUrl) {
        String path = backUrl == null ? "" : backUrl.split("\\?", 2)[0];
        if (path.contains("/reader/activities")) {
            return "activities";
        }
        if (path.contains("/writer/publishing")) {
            return "publishing";
        }
        if (path.contains("/writer/novels") || NOVEL_DETAIL_PATH.matcher(path).matches()) {
            return path.contains("/writer/") ? "writer" : "novels";
        }
        if (path.contains("/admin/fundings")) {
            return "admin-fundings";
        }
        if (path.contains("/admin/orders")) {
            return "admin-orders";
        }
        return "funding";
    }

    private String resolveParticipateHint(
            FundingCampaign campaign,
            boolean ownCampaign,
            boolean alreadyParticipated,
            boolean paidParticipation,
            boolean openForJoin,
            BookOrderStatus memberOrderStatus
    ) {
        if (alreadyParticipated) {
            if (memberOrderStatus != null) {
                return "이미 이 펀딩에 참여했습니다. " + memberOrderStatus.progressNote();
            }
            if (campaign.getStatus() == FundingCampaignStatus.SUCCESS && campaign.isAwaitingApproval()) {
                return "이미 이 펀딩에 참여했습니다. 운영자 승인 대기 중입니다.";
            }
            if (campaign.getStatus() == FundingCampaignStatus.SUCCESS && campaign.isApproved()) {
                return "이미 이 펀딩에 참여했습니다. 성공 마감 후 환불은 불가합니다.";
            }
            if (campaign.getStatus() == FundingCampaignStatus.FAILED) {
                return campaign.isApproved()
                        ? "이미 이 펀딩에 참여했습니다. 실패 승인으로 환불되었습니다."
                        : "이미 이 펀딩에 참여했습니다. 실패 마감 · 운영자 승인 대기 중입니다.";
            }
            if (!paidParticipation) {
                return "이미 환불한 펀딩입니다. 같은 펀딩에는 다시 참여할 수 없습니다.";
            }
            return "이미 이 펀딩에 참여했습니다. 진행 중에는 환불할 수 있습니다.";
        }
        if (ownCampaign) {
            return "본인 작품의 펀딩에는 참여할 수 없습니다.";
        }
        if (campaign.getStatus() != FundingCampaignStatus.IN_PROGRESS) {
            return "이 펀딩은 진행 중이 아닙니다.";
        }
        if (!openForJoin) {
            return "펀딩 기간이 아니어서 지금은 참여할 수 없습니다.";
        }
        return "참여 수량을 선택해 결제할 수 있습니다. 같은 펀딩에는 한 번만 참여할 수 있습니다. "
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
