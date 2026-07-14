package com.company.assistant.directory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees")
public class DirectoryController {

    @GetMapping("/ping")
    public String ping() {
        return "directory module OK";
    }
}