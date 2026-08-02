package com.novelkeep.home.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(ResponseStatusException ex) {
        return errorView(ex.getStatusCode(), ex.getReason());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", "요청하신 페이지를 찾을 수 없습니다. 비공개·삭제되었거나 권한이 없을 수 있습니다.");
        return "error";
    }

    private ModelAndView errorView(HttpStatusCode statusCode, String reason) {
        int status = statusCode != null ? statusCode.value() : 500;
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("status", status);
        mav.addObject("message", resolveMessage(status, reason));
        return mav;
    }

    private String resolveMessage(int status, String reason) {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        if (status == 404) {
            return "요청하신 페이지를 찾을 수 없습니다. 비공개·삭제되었거나 권한이 없을 수 있습니다.";
        }
        if (status == 403) {
            return "이 페이지에 접근할 권한이 없습니다.";
        }
        return "요청을 처리하는 중 문제가 발생했습니다.";
    }
}
