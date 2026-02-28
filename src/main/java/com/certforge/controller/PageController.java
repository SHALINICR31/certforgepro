package com.certforge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // App page — freely served; JavaScript checks token and redirects if missing
    @GetMapping("/app")
    public String app() {
        return "app";
    }

    // Public certificate verification page
    @GetMapping("/verify")
    public String verify() {
        return "verify";
    }
}
