package com.novelkeep.novel.controller;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.service.EpisodeCommentService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class ReaderCommentController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final EpisodeCommentService commentService;

    public ReaderCommentController(EpisodeCommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/reader/comments")
    public String myComments(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null || role == ExperienceRole.ADMIN) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("comments", commentService.listMyComments(memberId));
        model.addAttribute("navActive", "my-comments");
        return "reader/comments";
    }
}
