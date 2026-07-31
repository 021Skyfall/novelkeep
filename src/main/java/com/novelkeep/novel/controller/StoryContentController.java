package com.novelkeep.novel.controller;

import java.util.List;
import java.util.Map;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.EpisodeCommentForm;
import com.novelkeep.novel.dto.EpisodeForm;
import com.novelkeep.novel.dto.EpisodeNavigation;
import com.novelkeep.novel.dto.StoryContentActionResult;
import com.novelkeep.novel.dto.StoryPartForm;
import com.novelkeep.novel.service.EpisodeBookmarkService;
import com.novelkeep.novel.service.EpisodeCommentService;
import com.novelkeep.novel.service.StoryContentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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
public class StoryContentController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final StoryContentService storyContentService;
    private final EpisodeBookmarkService bookmarkService;
    private final EpisodeCommentService commentService;

    public StoryContentController(
            StoryContentService storyContentService,
            EpisodeBookmarkService bookmarkService,
            EpisodeCommentService commentService
    ) {
        this.storyContentService = storyContentService;
        this.bookmarkService = bookmarkService;
        this.commentService = commentService;
    }

    @GetMapping("/writer/novels/{novelId}/contents")
    public String contents(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Novel novel = storyContentService.findOwnedNovelWithContents(novelId, memberId);
        model.addAttribute("novel", novel);
        model.addAttribute("latestParts", storyContentService.latestParts(novel));
        model.addAttribute("storyPartForm", new StoryPartForm());
        model.addAttribute("partStatuses", StoryPartStatus.values());
        model.addAttribute("commentCounts", commentCountsFor(novel));
        model.addAttribute("navActive", "writer");
        return "writer/novels/contents";
    }

    private Map<Long, Long> commentCountsFor(Novel novel) {
        return commentService.countByEpisodeIds(
                novel.getParts().stream()
                        .flatMap(part -> part.getEpisodes().stream())
                        .map(Episode::getId)
                        .toList()
        );
    }

    @GetMapping("/writer/novels/{novelId}/detail-parts/partial")
    public String detailPartsPartial(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Novel novel = storyContentService.findOwnedNovelWithContents(novelId, memberId);
        model.addAttribute("novel", novel);
        model.addAttribute("latestParts", storyContentService.latestParts(novel));
        model.addAttribute("owned", true);
        model.addAttribute("privileged", true);
        model.addAttribute("partStatuses", StoryPartStatus.values());
        model.addAttribute("episodeStatuses", EpisodeStatus.values());
        model.addAttribute("commentCounts", commentCountsFor(novel));
        return "novels/fragments/detail-parts :: partsPanel";
    }

    @PostMapping("/writer/novels/{novelId}/parts")
    public Object createPart(
            @PathVariable Long novelId,
            @Valid @ModelAttribute("storyPartForm") StoryPartForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        if (bindingResult.hasErrors()) {
            String message = firstFieldError(bindingResult, "권(부) 제목과 상태를 확인해 주세요.");
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(message, novelId));
            }
            Novel novel = storyContentService.findOwnedNovelWithContents(novelId, memberId);
            model.addAttribute("novel", novel);
            model.addAttribute("latestParts", storyContentService.latestParts(novel));
            model.addAttribute("partStatuses", StoryPartStatus.values());
            model.addAttribute("navActive", "writer");
            model.addAttribute("partFormError", true);
            return "writer/novels/contents";
        }
        try {
            storyContentService.createPart(novelId, memberId, form);
            String message = "새 권(부)이 추가되었습니다. 이제 회차를 등록할 수 있습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(
                        StoryContentActionResult.success("partCreated", message, novelId)
                );
            }
            redirectAttributes.addFlashAttribute("contentLabel", "권(부) 추가");
            redirectAttributes.addFlashAttribute("contentMessage", message);
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(resolveMessage(ex), novelId));
            }
            redirectAttributes.addFlashAttribute("contentErrorLabel", "권(부) 추가 실패");
            redirectAttributes.addFlashAttribute("contentError", resolveMessage(ex));
        }
        return "redirect:/writer/novels/" + novelId + "/contents";
    }

    @PostMapping("/writer/parts/{partId}/edit")
    public Object updatePart(
            @PathVariable Long partId,
            @Valid @ModelAttribute("storyPartForm") StoryPartForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        StoryPart part = storyContentService.findOwnedPart(partId, memberId);
        Long novelId = part.getNovel().getId();
        if (bindingResult.hasErrors()) {
            String message = firstFieldError(bindingResult, "권(부) 제목과 상태를 확인해 주세요.");
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(message, novelId));
            }
            redirectAttributes.addFlashAttribute("contentErrorLabel", "권(부) 수정 실패");
            redirectAttributes.addFlashAttribute("contentError", message);
            return "redirect:/writer/novels/" + novelId + "/contents";
        }
        try {
            storyContentService.updatePart(partId, memberId, form);
            String message = "권(부) 제목과 상태가 저장되었습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(
                        StoryContentActionResult.success("partUpdated", message, novelId)
                );
            }
            redirectAttributes.addFlashAttribute("contentLabel", "권(부) 수정");
            redirectAttributes.addFlashAttribute("contentMessage", message);
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(resolveMessage(ex), novelId));
            }
            redirectAttributes.addFlashAttribute("contentErrorLabel", "권(부) 수정 실패");
            redirectAttributes.addFlashAttribute("contentError", resolveMessage(ex));
        }
        return "redirect:/writer/novels/" + novelId + "/contents";
    }

    @PostMapping("/writer/parts/{partId}/delete")
    public Object deletePart(
            @PathVariable Long partId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        StoryPart part = storyContentService.findOwnedPart(partId, memberId);
        Long novelId = part.getNovel().getId();
        try {
            storyContentService.deletePart(partId, memberId);
            String message = "선택한 권(부)이 삭제되고 권 순서가 다시 정렬되었습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(
                        StoryContentActionResult.success("partDeleted", message, novelId)
                );
            }
            redirectAttributes.addFlashAttribute("contentLabel", "권(부) 삭제");
            redirectAttributes.addFlashAttribute("contentMessage", message);
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(resolveMessage(ex), novelId));
            }
            redirectAttributes.addFlashAttribute("contentErrorLabel", "권(부) 삭제 실패");
            redirectAttributes.addFlashAttribute("contentError", resolveMessage(ex));
        }
        return "redirect:/writer/novels/" + novelId + "/contents";
    }

    @GetMapping("/writer/parts/{partId}/episodes/new")
    public String createEpisodeForm(
            @PathVariable Long partId,
            @RequestParam(required = false) String from,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        StoryPart part = storyContentService.findOwnedPart(partId, memberId);
        model.addAttribute("episodeForm", new EpisodeForm());
        addEpisodeFormOptions(model, part, null, false, false, isFromDetail(from));
        return "writer/episodes/form";
    }

    @PostMapping("/writer/parts/{partId}/episodes")
    public Object createEpisode(
            @PathVariable Long partId,
            @RequestParam(required = false) String from,
            @Valid @ModelAttribute("episodeForm") EpisodeForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        StoryPart part = storyContentService.findOwnedPart(partId, memberId);
        Long novelId = part.getNovel().getId();
        boolean fromDetail = isFromDetail(from);
        if (bindingResult.hasErrors()) {
            String message = firstFieldError(bindingResult, "회차 제목·본문·공개 상태를 확인해 주세요.");
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(message, novelId));
            }
            addEpisodeFormOptions(model, part, null, false, false, fromDetail);
            return "writer/episodes/form";
        }
        try {
            storyContentService.createEpisode(partId, memberId, form);
            String message = "새 회차가 등록되었습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(
                        StoryContentActionResult.success("episodeCreated", message, novelId)
                );
            }
            redirectAttributes.addFlashAttribute("contentLabel", "회차 등록");
            redirectAttributes.addFlashAttribute("contentMessage", message);
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(resolveMessage(ex), novelId));
            }
            redirectAttributes.addFlashAttribute("contentErrorLabel", "회차 등록 실패");
            redirectAttributes.addFlashAttribute("contentError", resolveMessage(ex));
            if (fromDetail) {
                return "redirect:/novels/" + novelId + "?from=writer";
            }
            return "redirect:/writer/novels/" + novelId + "/contents";
        }
        if (fromDetail) {
            return "redirect:/novels/" + novelId + "?from=writer&episodeCreated=true";
        }
        return "redirect:/writer/novels/" + novelId + "/contents";
    }

    @GetMapping("/writer/episodes/{episodeId}/edit")
    public String editEpisodeForm(
            @PathVariable Long episodeId,
            @RequestParam(required = false) String from,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Episode episode = storyContentService.findOwnedEpisode(episodeId, memberId);
        model.addAttribute("episodeForm", EpisodeForm.from(episode));
        addEpisodeFormOptions(
                model,
                episode.getStoryPart(),
                episodeId,
                true,
                isFromRead(from),
                isFromDetail(from)
        );
        return "writer/episodes/form";
    }

    @PostMapping("/writer/episodes/{episodeId}/edit")
    public String updateEpisode(
            @PathVariable Long episodeId,
            @RequestParam(required = false) String from,
            @Valid @ModelAttribute("episodeForm") EpisodeForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Episode episode = storyContentService.findOwnedEpisode(episodeId, memberId);
        if (bindingResult.hasErrors()) {
            addEpisodeFormOptions(
                    model,
                    episode.getStoryPart(),
                    episodeId,
                    true,
                    isFromRead(from),
                    isFromDetail(from)
            );
            return "writer/episodes/form";
        }
        storyContentService.updateEpisode(episodeId, memberId, form);
        redirectAttributes.addFlashAttribute("contentLabel", "회차 수정");
        redirectAttributes.addFlashAttribute("contentMessage", "회차 제목·본문·공개 상태가 저장되었습니다.");
        if (isFromRead(from)) {
            return "redirect:/episodes/" + episodeId + "?updated=true";
        }
        if (isFromDetail(from)) {
            return "redirect:/novels/" + episode.getStoryPart().getNovel().getId()
                    + "?from=writer&episodeUpdated=true";
        }
        return "redirect:/writer/novels/" + episode.getStoryPart().getNovel().getId() + "/contents";
    }

    @PostMapping("/writer/novels/{novelId}/episodes/bulk")
    public Object bulkEpisodes(
            @PathVariable Long novelId,
            @RequestParam String action,
            @RequestParam(name = "episodeIds", required = false) List<Long> episodeIds,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        try {
            String normalized = action == null ? "" : action.trim().toUpperCase();
            String message;
            switch (normalized) {
                case "PUBLISH" -> {
                    storyContentService.bulkChangeEpisodeStatus(
                            novelId, memberId, episodeIds, EpisodeStatus.PUBLISHED
                    );
                    message = "선택한 회차를 공개로 변경했습니다.";
                }
                case "UNPUBLISH" -> {
                    storyContentService.bulkChangeEpisodeStatus(
                            novelId, memberId, episodeIds, EpisodeStatus.UNPUBLISHED
                    );
                    message = "선택한 회차를 미공개로 변경했습니다.";
                }
                case "DELETE" -> {
                    storyContentService.bulkDeleteEpisodes(novelId, memberId, episodeIds);
                    message = "선택한 회차를 삭제하고 회차 번호를 다시 정렬했습니다.";
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 일괄 작업입니다.");
            }
            if (wantsJson(request)) {
                return ResponseEntity.ok(
                        StoryContentActionResult.success("episodeBulk", message, novelId)
                );
            }
            return "redirect:/novels/" + novelId + "?from=writer";
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(resolveMessage(ex), novelId));
            }
            return "redirect:/novels/" + novelId + "?from=writer";
        }
    }

    @PostMapping("/writer/episodes/{episodeId}/delete")
    public Object deleteEpisode(
            @PathVariable Long episodeId,
            @RequestParam(required = false) String from,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (!canWrite(role) || memberId == null) {
            return unauthorized(request);
        }
        try {
            Long novelId = storyContentService.deleteEpisode(episodeId, memberId);
            String message = "선택한 회차가 삭제되고 회차 번호가 다시 정렬되었습니다.";
            if (wantsJson(request)) {
                return ResponseEntity.ok(
                        StoryContentActionResult.success("episodeDeleted", message, novelId)
                );
            }
            redirectAttributes.addFlashAttribute("contentLabel", "회차 삭제");
            redirectAttributes.addFlashAttribute("contentMessage", message);
            if (isFromRead(from) || isFromDetail(from)) {
                return "redirect:/novels/" + novelId + "?from=writer&episodeDeleted=true";
            }
            return "redirect:/writer/novels/" + novelId + "/contents";
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.badRequest()
                        .body(StoryContentActionResult.failure(resolveMessage(ex), null));
            }
            redirectAttributes.addFlashAttribute("contentErrorLabel", "회차 삭제 실패");
            redirectAttributes.addFlashAttribute("contentError", resolveMessage(ex));
            return "redirect:/writer/novels";
        }
    }

    @GetMapping("/episodes/{episodeId}")
    public String readEpisode(
            @PathVariable Long episodeId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Episode episode = storyContentService.findReadableEpisode(episodeId, memberId, role);
        Novel novel = episode.getStoryPart().getNovel();
        EpisodeNavigation navigation = storyContentService.buildNavigation(episodeId, memberId, role);
        boolean owned = novel.isOwnedBy(memberId);

        model.addAttribute("episode", episode);
        model.addAttribute("novel", novel);
        model.addAttribute("part", episode.getStoryPart());
        model.addAttribute("navigation", navigation);
        model.addAttribute("owned", owned);
        model.addAttribute("multiPart", novel.isMultiPart());
        model.addAttribute(
                "bookmarked",
                bookmarkService.isBookmarkedEpisode(novel.getId(), episodeId, memberId)
        );
        model.addAttribute("comments", commentService.listReadable(episodeId, memberId, role));
        model.addAttribute("commentForm", new EpisodeCommentForm());
        model.addAttribute("currentMemberId", memberId);
        model.addAttribute("navActive", "novels");
        return "episodes/read";
    }

    private void addEpisodeFormOptions(Model model, StoryPart part, Long episodeId, boolean editing) {
        addEpisodeFormOptions(model, part, episodeId, editing, false, false);
    }

    private void addEpisodeFormOptions(
            Model model,
            StoryPart part,
            Long episodeId,
            boolean editing,
            boolean fromRead,
            boolean fromDetail
    ) {
        model.addAttribute("part", part);
        model.addAttribute("novel", part.getNovel());
        model.addAttribute("episodeId", episodeId);
        model.addAttribute("editing", editing);
        model.addAttribute("fromRead", fromRead);
        model.addAttribute("fromDetail", fromDetail);
        model.addAttribute("episodeStatuses", EpisodeStatus.values());
        model.addAttribute("navActive", "writer");
    }

    private boolean isFromRead(String from) {
        return "read".equalsIgnoreCase(from);
    }

    private boolean isFromDetail(String from) {
        return "detail".equalsIgnoreCase(from);
    }

    private boolean canWrite(ExperienceRole role) {
        return role == ExperienceRole.WRITER || role == ExperienceRole.ADMIN;
    }

    private Object unauthorized(HttpServletRequest request) {
        if (wantsJson(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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

    private String firstFieldError(BindingResult bindingResult, String fallback) {
        if (bindingResult.getFieldError() != null
                && bindingResult.getFieldError().getDefaultMessage() != null) {
            return bindingResult.getFieldError().getDefaultMessage();
        }
        return fallback;
    }

    private String resolveMessage(ResponseStatusException ex) {
        if (ex.getReason() != null && !ex.getReason().isBlank()) {
            return ex.getReason();
        }
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return "요청을 처리할 수 없습니다.";
        }
        return "요청한 콘텐츠를 찾을 수 없습니다.";
    }
}
