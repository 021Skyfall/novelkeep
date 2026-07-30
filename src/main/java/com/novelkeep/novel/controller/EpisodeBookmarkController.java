package com.novelkeep.novel.controller;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.dto.EpisodeBookmarkResult;
import com.novelkeep.novel.service.EpisodeBookmarkService;

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

@Controller
public class EpisodeBookmarkController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final EpisodeBookmarkService bookmarkService;

    public EpisodeBookmarkController(EpisodeBookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @GetMapping("/bookmarks")
    public String bookmarks(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("bookmarks", bookmarkService.findReadableBookmarks(memberId, role));
        model.addAttribute("navActive", "bookmarks");
        return "bookmarks/list";
    }

    @PostMapping("/bookmarks/{novelId}/delete")
    public String delete(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        bookmarkService.delete(memberId, novelId);
        return "redirect:/bookmarks?deleted=true";
    }

    @PostMapping("/episodes/{episodeId}/bookmark")
    public Object toggle(
            @PathVariable Long episodeId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            HttpServletRequest request
    ) {
        if (role == null || memberId == null) {
            if (wantsJson(request)) {
                return ResponseEntity.status(401).build();
            }
            return "redirect:/?roleRequired=true";
        }

        try {
            EpisodeBookmarkResult result = bookmarkService.toggle(episodeId, memberId, role);
            if (wantsJson(request)) {
                return ResponseEntity.ok(result);
            }
            return "redirect:/episodes/" + episodeId + (result.isBookmarked()
                    ? "?bookmarked=true"
                    : "?bookmarkRemoved=true");
        } catch (ResponseStatusException ex) {
            if (wantsJson(request)) {
                return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
            }
            throw ex;
        }
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
