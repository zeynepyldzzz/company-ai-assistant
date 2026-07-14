package com.company.assistant.menu;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menus")
public class MenuController {

    @GetMapping("/ping")
    public String ping() {
        return "menu module OK";
    }
}