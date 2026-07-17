package com.company.assistant.directory;

import com.company.assistant.common.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PhonebookService {

    private static final Logger log = LoggerFactory.getLogger(PhonebookService.class);

    private final EmployeeRepository employeeRepository;

    public PhonebookService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public PagedResponse<PhonebookEntryResponse> searchPhonebook(String search, int page, int pageSize) {
        Page<Employee> result = employeeRepository.searchPhonebook(search, PageRequest.of(page, pageSize));
        return new PagedResponse<>(
                result.getContent().stream().map(PhonebookEntryResponse::new).toList(),
                page,
                pageSize,
                result.getTotalElements()
        );
    }

    // MVP: gercek telefon santrali entegrasyonu yok, tetikleme sadece loglaniyor.
    public CallTriggerResponse triggerCall(String extension) {
        employeeRepository.findByPhone(extension)
                .orElseThrow(() -> new ExtensionNotFoundException("Dahili numara bulunamadi: " + extension));

        log.info("Click-to-call tetiklendi: extension={}", extension);

        return new CallTriggerResponse(extension, "triggered", Instant.now());
    }
}
