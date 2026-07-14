package com.company.assistant.announcement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/announcements")
public class AnnouncementsController {

    @GetMapping("/ping")
    public String ping() {
        return "announcement module OK";
    }
}