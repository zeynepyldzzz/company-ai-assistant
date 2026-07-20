package com.company.assistant.directory;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.common.PagedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/phonebook")
public class PhonebookController {

    private final PhonebookService phonebookService;

    public PhonebookController(PhonebookService phonebookService) {
        this.phonebookService = phonebookService;
    }

    @GetMapping
    public PagedResponse<PhonebookEntryResponse> searchPhonebook(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return phonebookService.searchPhonebook(search, page, pageSize);
    }

    @PostMapping("/{extension}/call")
    public ResponseEntity<CallTriggerResponse> triggerCall(@PathVariable String extension) {
        return ResponseEntity.accepted().body(phonebookService.triggerCall(extension));
    }

    @ExceptionHandler(ExtensionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ExtensionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("EXTENSION_NOT_FOUND", ex.getMessage()));
    }
}
