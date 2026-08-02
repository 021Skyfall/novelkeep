package com.novelkeep.novel.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.dto.WriterFundingForm;
import com.novelkeep.funding.service.FundingCampaignService;
import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelGenre;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.NovelActionResult;
import com.novelkeep.novel.dto.NovelForm;
import com.novelkeep.novel.dto.NovelSearchCriteria;
import com.novelkeep.novel.domain.EpisodeBookmark;
import com.novelkeep.novel.service.EpisodeBookmarkService;
import com.novelkeep.novel.service.EpisodeCommentService;
import com.novelkeep.novel.service.NovelService;
import com.novelkeep.novel.service.StoryContentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NovelController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final NovelService novelService;
    private final StoryContentService storyContentService;
    private final EpisodeBookmarkService bookmarkService;
    private final EpisodeCommentService commentService;
    private final FundingCampaignService fundingCampaignService;

    public NovelController(
            NovelService novelService,
            StoryContentService storyContentService,
            EpisodeBookmarkService bookmarkService,
            EpisodeCommentService commentService,
            FundingCampaignService fundingCampaignService
    ) {
        this.novelService = novelService;
        this.storyContentService = storyContentService;
        this.bookmarkService = bookmarkService;
        this.commentService = commentService;
        this.fundingCampaignService = fundingCampaignService;
    }

    @GetMapping("/novels")
    public Object publicList(
            NovelSearchCriteria criteria,
            @RequestParam(required = false) Integer page,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            Model model
    ) {
        if (role == null || memberId == null) {
            if (isPartialRequest(request)) {
                return ResponseEntity.status(401).build();
            }
            return "redirect:/?roleRequired=true";
        }
        if (page != null) {
            criteria.setPage(page);
        }
        applyEntrySortDefaults(criteria, request);
        if (role != ExperienceRole.ADMIN) {
            criteria.setVisibility(null);
        }

        Page<Novel> novels = novelService.searchPublic(criteria, role, memberId);
        Set<Long> recommendedIds = novelService.findRecommendedNovelIds(memberId, novels);
        Set<Long> favoritedIds = novelService.findFavoritedNovelIds(memberId, novels);
        model.addAttribute("novels", novels);
        model.addAttribute("openFundingByNovelId", fundingCampaignService.findOpenCampaignsGroupedByNovelId(
                novels.getContent().stream().map(Novel::getId).toList()
        ));
        model.addAttribute("criteria", criteria);
        model.addAttribute("allGenres", NovelGenre.values());
        model.addAttribute("visibilities", NovelVisibility.values());
        model.addAttribute("recommendedIds", recommendedIds);
        model.addAttribute("favoritedIds", favoritedIds);
        model.addAttribute("currentMemberId", memberId);
        model.addAttribute("isAdmin", role == ExperienceRole.ADMIN);
        model.addAttribute("navActive", resolveNavActive(criteria));
        if (isPartialRequest(request)) {
            return "novels/list :: asyncContent";
        }
        return "novels/list";
    }

    @GetMapping("/novels/{novelId}")
    public String detail(
            @PathVariable Long novelId,
            @RequestParam(required = false) String from,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Novel novel = storyContentService.findReadableNovelWithContents(novelId, memberId, role);
        boolean owned = novel.isOwnedBy(memberId);
        boolean canManageContent = owned && role == ExperienceRole.WRITER;
        boolean publicNovel = novel.getVisibility() == NovelVisibility.PUBLIC;
        boolean privileged = owned || role == ExperienceRole.ADMIN;
        model.addAttribute("novel", novel);
        model.addAttribute("latestParts", storyContentService.latestParts(novel, privileged));
        model.addAttribute("owned", owned);
        model.addAttribute("canManageContent", canManageContent);
        model.addAttribute("privileged", privileged);
        model.addAttribute("recommended", novelService.hasRecommended(novelId, memberId));
        model.addAttribute("favorited", novelService.hasFavorited(novelId, memberId));
        model.addAttribute("canRecommend", publicNovel && !owned);
        model.addAttribute("canFavorite", publicNovel);
        EpisodeBookmark continueBookmark = bookmarkService.findReadableBookmark(novelId, memberId, role);
        model.addAttribute("continueBookmark", continueBookmark);
        model.addAttribute("commentCounts", commentCountsFor(novel));
        model.addAttribute("fromWriter", "writer".equalsIgnoreCase(from));
        var openFundingByPartId = fundingCampaignService.findOpenCampaignsByPartIds(
                novel.getParts().stream().map(part -> part.getId()).toList()
        );
        model.addAttribute("openFundingByPartId", openFundingByPartId);
        model.addAttribute(
                "participatedCampaignIds",
                fundingCampaignService.findParticipatedCampaignIds(
                        memberId,
                        openFundingByPartId.values().stream().map(c -> c.getId()).toList()
                )
        );
        if (canManageContent) {
            model.addAttribute("partStatuses", StoryPartStatus.values());
            model.addAttribute("episodeStatuses", EpisodeStatus.values());
            if (!model.containsAttribute("fundingForm")) {
                model.addAttribute("fundingForm", WriterFundingForm.defaults());
            }
            model.addAttribute("minTargetQuantity", FundingGuide.MIN_TARGET_QUANTITY);
            model.addAttribute("minDurationDays", FundingGuide.MIN_DURATION_DAYS);
            model.addAttribute("guideVolumeChars", FundingGuide.GUIDE_VOLUME_CHARS);
            model.addAttribute("guideVolumeText", FundingGuide.GUIDE_VOLUME_TEXT);
            model.addAttribute("fundingStartNotices", FundingGuide.startNotices());
        }
        return "novels/detail";
    }

    private Map<Long, Long> commentCountsFor(Novel novel) {
        List<Long> episodeIds = novel.getParts().stream()
                .flatMap(part -> part.getEpisodes().stream())
                .map(Episode::getId)
                .toList();
        return commentService.countByEpisodeIds(episodeIds);
    }

    @PostMapping("/novels/{novelId}/recommend")
    public Object toggleRecommend(
            @PathVariable Long novelId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String returnTo,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request
    ) {
        if (role == null || memberId == null) {
            return redirectOrUnauthorized(request);
        }
        novelService.findReadableNovel(novelId, memberId, role);
        NovelActionResult result = novelService.toggleRecommendation(novelId, memberId);
        if (wantsJson(request)) {
            return ResponseEntity.ok(result);
        }
        return redirectAfterToggle(novelId, from, returnTo);
    }

    @PostMapping("/novels/{novelId}/favorite")
    public Object toggleFavorite(
            @PathVariable Long novelId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String returnTo,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request
    ) {
        if (role == null || memberId == null) {
            return redirectOrUnauthorized(request);
        }
        novelService.findReadableNovel(novelId, memberId, role);
        NovelActionResult result = novelService.toggleFavorite(novelId, memberId);
        if (wantsJson(request)) {
            return ResponseEntity.ok(result);
        }
        return redirectAfterToggle(novelId, from, returnTo);
    }

    @GetMapping("/writer/novels")
    public Object writerList(
            NovelSearchCriteria criteria,
            @RequestParam(required = false) Integer page,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            if (isPartialRequest(request)) {
                return ResponseEntity.status(401).build();
            }
            return "redirect:/?roleRequired=true";
        }
        if (page != null) {
            criteria.setPage(page);
        }
        applyEntrySortDefaults(criteria, request);

        Page<Novel> novels = novelService.searchOwned(criteria, memberId);
        model.addAttribute("novels", novels);
        model.addAttribute("openFundingByNovelId", fundingCampaignService.findOpenCampaignsGroupedByNovelId(
                novels.getContent().stream().map(Novel::getId).toList()
        ));
        model.addAttribute("criteria", criteria);
        model.addAttribute("allGenres", NovelGenre.values());
        model.addAttribute("visibilities", NovelVisibility.values());
        model.addAttribute("navActive", "writer");
        if (isPartialRequest(request)) {
            return "writer/novels/list :: asyncContent";
        }
        return "writer/novels/list";
    }

    @GetMapping("/writer/novels/new")
    public String createForm(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("novelForm", new NovelForm());
        addFormOptions(model, false, null);
        return "writer/novels/form";
    }

    @PostMapping("/writer/novels")
    public String create(
            @Valid @ModelAttribute("novelForm") NovelForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        if (bindingResult.hasErrors()) {
            addFormOptions(model, false, null);
            return "writer/novels/form";
        }

        Long novelId = novelService.create(memberId, form);
        return "redirect:/novels/" + novelId + "?created=true&from=writer";
    }

    @GetMapping("/writer/novels/{novelId}/edit")
    public String editForm(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Novel novel = novelService.findOwnedNovel(novelId, memberId);
        model.addAttribute("novelForm", NovelForm.from(novel));
        addFormOptions(model, true, novelId);
        return "writer/novels/form";
    }

    @PostMapping("/writer/novels/{novelId}/edit")
    public String update(
            @PathVariable Long novelId,
            @Valid @ModelAttribute("novelForm") NovelForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        if (bindingResult.hasErrors()) {
            addFormOptions(model, true, novelId);
            return "writer/novels/form";
        }

        novelService.update(novelId, memberId, form);
        return "redirect:/novels/" + novelId + "?updated=true&from=writer";
    }

    @PostMapping("/writer/novels/{novelId}/delete")
    public String delete(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        try {
            novelService.delete(novelId, memberId);
            return "redirect:/writer/novels?deleted=true";
        } catch (ResponseStatusException ex) {
            String reason = ex.getReason();
            redirectAttributes.addFlashAttribute(
                    "deleteError",
                    reason == null || reason.isBlank() ? "작품을 삭제할 수 없습니다." : reason
            );
            return "redirect:/novels/" + novelId + "?from=writer";
        }
    }

    private String redirectAfterToggle(Long novelId, String from, String returnTo) {
        if ("browse".equalsIgnoreCase(returnTo)) {
            return "redirect:/novels";
        }
        if ("favorites".equalsIgnoreCase(returnTo)) {
            return "redirect:/novels?favorite=true";
        }
        if ("main".equalsIgnoreCase(returnTo)) {
            return "redirect:/main#popular";
        }
        if ("writer".equalsIgnoreCase(returnTo)) {
            return "redirect:/writer/novels";
        }
        if ("writer".equalsIgnoreCase(from)) {
            return "redirect:/novels/" + novelId + "?from=writer";
        }
        return "redirect:/novels/" + novelId;
    }

    private Object redirectOrUnauthorized(HttpServletRequest request) {
        if (wantsJson(request)) {
            return ResponseEntity.status(401).build();
        }
        return "redirect:/?roleRequired=true";
    }

    private boolean wantsJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private boolean isPartialRequest(HttpServletRequest request) {
        String partial = request.getHeader("X-Novelkeep-Partial");
        if ("1".equals(partial) || "true".equalsIgnoreCase(partial)) {
            return true;
        }
        return "1".equals(request.getParameter("partial"))
                || "true".equalsIgnoreCase(request.getParameter("partial"));
    }

    private void applyEntrySortDefaults(NovelSearchCriteria criteria, HttpServletRequest request) {
        if (!request.getParameterMap().containsKey("sort")) {
            criteria.setSort("latest");
            criteria.setSortDir("DESC");
            return;
        }
        if (criteria.getSort() == null || criteria.getSort().isBlank()) {
            criteria.setSort("");
            criteria.setSortDir("");
        }
    }

    private String resolveNavActive(NovelSearchCriteria criteria) {
        if (criteria.isFavoriteOnly()) {
            return "favorites";
        }
        if (criteria.isCompletedProgress()
                && isBlank(criteria.getKeyword())
                && !criteria.hasGenres()
                && criteria.getVisibility() == null) {
            return "completed";
        }
        if (!criteria.isTitleSort()
                && !criteria.isRecommendSort()
                && isBlank(criteria.getKeyword())
                && !criteria.hasGenres()
                && isBlank(criteria.getProgress())
                && criteria.getVisibility() == null
                && !criteria.isFavoriteOnly()
                && "latest".equalsIgnoreCase(criteria.getSort())) {
            return "new";
        }
        return "novels";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean canWrite(ExperienceRole role) {
        return role == ExperienceRole.WRITER;
    }

    private void addFormOptions(Model model, boolean editing, Long novelId) {
        model.addAttribute("allGenres", NovelGenre.values());
        model.addAttribute("maxGenres", NovelGenre.MAX_PER_NOVEL);
        model.addAttribute("statuses", NovelStatus.values());
        model.addAttribute("visibilities", NovelVisibility.values());
        model.addAttribute("editing", editing);
        model.addAttribute("novelId", novelId);
        model.addAttribute("navActive", "writer");
    }
}
