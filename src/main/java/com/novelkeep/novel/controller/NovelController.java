package com.novelkeep.novel.controller;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.PartMode;
import com.novelkeep.novel.dto.NovelForm;
import com.novelkeep.novel.service.NovelService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class NovelController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final NovelService novelService;

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    @GetMapping("/novels")
    public String publicList(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            Model model
    ) {
        if (role == null) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("novels", novelService.findPublicNovels());
        model.addAttribute("roleName", role.getDisplayName());
        return "novels/list";
    }

    @GetMapping("/novels/{novelId}")
    public String detail(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (role == null || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        Novel novel = novelService.findReadableNovel(novelId, memberId);
        model.addAttribute("novel", novel);
        model.addAttribute("owned", novel.getAuthor().getId().equals(memberId));
        model.addAttribute("roleName", role.getDisplayName());
        return "novels/detail";
    }

    @GetMapping("/writer/novels")
    public String writerList(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("novels", novelService.findOwnedNovels(memberId));
        model.addAttribute("roleName", role.getDisplayName());
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
        validateFirstPartTitle(form, bindingResult);
        if (bindingResult.hasErrors()) {
            addFormOptions(model, false, null);
            return "writer/novels/form";
        }

        Long novelId = novelService.create(memberId, form);
        return "redirect:/novels/" + novelId + "?created=true";
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
        return "redirect:/novels/" + novelId + "?updated=true";
    }

    @PostMapping("/writer/novels/{novelId}/delete")
    public String delete(
            @PathVariable Long novelId,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!canWrite(role) || memberId == null) {
            return "redirect:/?roleRequired=true";
        }
        novelService.delete(novelId, memberId);
        return "redirect:/writer/novels?deleted=true";
    }

    private boolean canWrite(ExperienceRole role) {
        return role == ExperienceRole.WRITER || role == ExperienceRole.ADMIN;
    }

    private void validateFirstPartTitle(NovelForm form, BindingResult bindingResult) {
        if (form.getPartMode() == PartMode.MULTI
                && (form.getFirstPartTitle() == null || form.getFirstPartTitle().isBlank())) {
            bindingResult.rejectValue("firstPartTitle", "required", "첫 부 제목을 입력해 주세요.");
        }
    }

    private void addFormOptions(Model model, boolean editing, Long novelId) {
        model.addAttribute("statuses", NovelStatus.values());
        model.addAttribute("partModes", PartMode.values());
        model.addAttribute("editing", editing);
        model.addAttribute("novelId", novelId);
    }
}
