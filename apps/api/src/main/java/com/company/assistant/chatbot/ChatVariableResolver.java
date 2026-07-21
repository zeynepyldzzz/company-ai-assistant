package com.company.assistant.chatbot;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

@Component
public class ChatVariableResolver {

    private static final Logger log = LoggerFactory.getLogger(ChatVariableResolver.class);

    private final EmployeeRepository employeeRepository;

    public ChatVariableResolver(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, String> resolve(Authentication authentication) {
        if (authentication == null) {
            return Map.of();
        }

        Employee employee;
        try {
            employee = employeeRepository.findById(Integer.valueOf(authentication.getName()))
                    .filter(Employee::isActive)
                    .orElse(null);
        } catch (NumberFormatException e) {
            log.warn("Authentication name sayisal degil: {}", authentication.getName());
            return Map.of();
        }

        if (employee == null) {
            return Map.of();
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("kullanici_adi", employee.getName());
        if (employee.getDepartment() != null) {
            variables.put("departman", employee.getDepartment().getName());
        } else {
            log.warn("Calisan {} icin departman tanimli degil.", employee.getId());
        }
        return variables;
    }
}