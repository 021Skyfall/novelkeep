package com.novelkeep.novel.controller;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.dto.EpisodeCommentForm;
import com.novelkeep.novel.dto.EpisodeCommentResult;
import com.novelkeep.novel.service.EpisodeCommentService;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EpisodeCommentController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final EpisodeCommentService commentService;

    public EpisodeCommentController(EpisodeCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/episodes/{episodeId}/comments/partial")
    public String partial(
            @PathVariable Long episodeId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("episode", commentService.findReadableEpisodeRef(episodeId, memberId, role));
        model.addAttribute("comments", commentService.listReadable(episodeId, memberId, role));
        model.addAttribute("commentForm", new EpisodeCommentForm());
        model.addAttribute("currentMemberId", memberId);
        return "episodes/fragments/comments :: commentsPanel";
    }

    @PostMapping("/episodes/{episodeId}/comments")
    public Object create(
            @PathVariable Long episodeId,
            @Valid @ModelAttribute("commentForm") EpisodeCommentForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (role == null || memberId == null) {
            return unauthorized(request);
        }
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "댓글을 확인한 뒤 다시 작성해 주세요.";
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(EpisodeCommentResult.failure(message, episodeId));
            }
            redirectAttributes.addFlashAttribute("commentError", message);
            return "redirect:/episodes/" + episodeId + "#comments";
        }
        commentService.create(episodeId, memberId, role, form);
        if (wantsJson(request)) {
            return ResponseEntity.ok(EpisodeCommentResult.success("saved", "댓글이 등록되었습니다.", episodeId));
        }
        redirectAttributes.addFlashAttribute("commentSaved", true);
        return "redirect:/episodes/" + episodeId + "#comments";
    }

    @PostMapping("/comments/{commentId}/edit")
    public Object update(
            @PathVariable Long commentId,
            @Valid @ModelAttribute("commentForm") EpisodeCommentForm form,
            BindingResult bindingResult,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (role == null || memberId == null) {
            return unauthorized(request);
        }
        Long episodeId = commentService.findOwnedEpisodeId(commentId, memberId);
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldError() != null
                    ? bindingResult.getFieldError().getDefaultMessage()
                    : "댓글을 확인한 뒤 다시 수정해 주세요.";
            if (wantsJson(request)) {
                return ResponseEntity.badRequest().body(EpisodeCommentResult.failure(message, episodeId));
            }
            redirectAttributes.addFlashAttribute("commentError", message);
            redirectAttributes.addFlashAttribute("editingCommentId", commentId);
            return "redirect:/episodes/" + episodeId + "#comments";
        }
        commentService.update(commentId, memberId, role, form);
        if (wantsJson(request)) {
            return ResponseEntity.ok(EpisodeCommentResult.success("updated", "댓글이 수정되었습니다.", episodeId));
        }
        redirectAttributes.addFlashAttribute("commentUpdated", true);
        return "redirect:/episodes/" + episodeId + "#comments";
    }

    @PostMapping("/comments/{commentId}/delete")
    public Object delete(
            @PathVariable Long commentId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (role == null || memberId == null) {
            return unauthorized(request);
        }
        Long episodeId = commentService.delete(commentId, memberId, role);
        if (wantsJson(request)) {
            return ResponseEntity.ok(EpisodeCommentResult.success("deleted", "댓글이 삭제되었습니다.", episodeId));
        }
        redirectAttributes.addFlashAttribute("commentDeleted", true);
        return "redirect:/episodes/" + episodeId + "#comments";
    }

    private Object unauthorized(HttpServletRequest request) {
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
}
