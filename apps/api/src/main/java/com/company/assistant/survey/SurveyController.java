package com.company.assistant.survey;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/surveys")
public class SurveyController {

    @GetMapping("/ping")
    public String ping() {
        return "survey module OK";
    }
}