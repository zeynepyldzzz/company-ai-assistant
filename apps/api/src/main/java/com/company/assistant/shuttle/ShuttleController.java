package com.company.assistant.shuttle;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shuttle-routes")
public class ShuttleController {

    @GetMapping("/ping")
    public String ping() {
        return "shuttle module OK";
    }
}