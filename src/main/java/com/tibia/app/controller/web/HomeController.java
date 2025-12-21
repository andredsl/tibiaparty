package com.tibia.app.controller.web;

import com.tibia.app.service.SessionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SessionService sessionService;

    public HomeController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/")
    public String home() {
        if (sessionService.isAuthenticated()) {
            return "redirect:/parties";
        }
        return "redirect:/auth/login";
    }
}
