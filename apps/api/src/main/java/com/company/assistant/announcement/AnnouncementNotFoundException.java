package com.company.assistant.announcement;

/** C-9 (#53): id ile bulunamayan duyuru icin. */
public class AnnouncementNotFoundException extends RuntimeException {

    public AnnouncementNotFoundException(String message) {
        super(message);
    }
}
