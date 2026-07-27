package com.company.assistant.directory;

/** #84 (Hafta 4): admin employee create/update'te gecersiz roleId icin. */
public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}
