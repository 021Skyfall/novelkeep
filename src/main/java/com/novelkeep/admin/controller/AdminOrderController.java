package com.novelkeep.admin.controller;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.dto.BookOrderSearchCriteria;
import com.novelkeep.order.service.BookOrderService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin")
public class AdminOrderController {

    private static final String SESSION_ROLE = "experienceRole";
    private static final String SESSION_MEMBER_ID = "memberId";

    private final BookOrderService bookOrderService;

    public AdminOrderController(BookOrderService bookOrderService) {
        this.bookOrderService = bookOrderService;
    }

    @GetMapping({"", "/"})
    public String index(
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/orders")
    public String orders(
            @ModelAttribute("criteria") BookOrderSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            Model model
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        model.addAttribute("orders", bookOrderService.search(criteria));
        model.addAttribute("statuses", BookOrderStatus.values());
        model.addAttribute("navActive", "admin-orders");
        return "admin/orders";
    }

    @PostMapping("/orders/{orderId}/status")
    public String advanceStatus(
            @PathVariable Long orderId,
            @ModelAttribute BookOrderSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId,
            RedirectAttributes redirectAttributes
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        try {
            bookOrderService.advanceStatus(orderId);
            redirectAttributes.addFlashAttribute("orderUpdated", true);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute(
                    "orderError",
                    ex.getReason() == null ? "주문 상태를 변경할 수 없습니다." : ex.getReason()
            );
        }
        return redirectWithCriteria(criteria);
    }

    @GetMapping("/orders/export.csv")
    public Object exportCsv(
            @ModelAttribute BookOrderSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        byte[] body = bookOrderService.exportCsv(criteria);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("novelkeep-orders.csv"))
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/orders/export.json")
    public Object exportJson(
            @ModelAttribute BookOrderSearchCriteria criteria,
            @SessionAttribute(name = SESSION_ROLE, required = false) ExperienceRole role,
            @SessionAttribute(name = SESSION_MEMBER_ID, required = false) Long memberId
    ) {
        if (!isOperator(role, memberId)) {
            return "redirect:/?roleRequired=true";
        }
        byte[] body = bookOrderService.exportJson(criteria);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("novelkeep-orders.json"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String redirectWithCriteria(BookOrderSearchCriteria criteria) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/orders");
        if (criteria.getNovelTitle() != null && !criteria.getNovelTitle().isBlank()) {
            builder.queryParam("novelTitle", criteria.getNovelTitle().trim());
        }
        if (criteria.getStatus() != null) {
            builder.queryParam("status", criteria.getStatus().name());
        }
        if (criteria.getOrderedFrom() != null) {
            builder.queryParam("orderedFrom", criteria.getOrderedFrom());
        }
        if (criteria.getOrderedTo() != null) {
            builder.queryParam("orderedTo", criteria.getOrderedTo());
        }
        return "redirect:" + builder.build().encode().toUriString();
    }

    private String attachment(String filename) {
        return ContentDisposition.attachment()
                .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private boolean isOperator(ExperienceRole role, Long memberId) {
        return role == ExperienceRole.ADMIN && memberId != null;
    }
}
