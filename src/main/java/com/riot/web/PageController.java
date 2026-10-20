package com.riot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    @GetMapping({"/", "/Main"})
    public String mainPage() {
        return "Main";
    }
}
