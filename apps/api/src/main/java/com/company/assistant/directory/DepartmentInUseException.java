package com.company.assistant.directory;

/**
 * #84 (Hafta 4): departman silinmek istendiginde hala calisan atanmissa
 * (FK RESTRICT ile veritabani hatasi vermek yerine anlamli 409 dondurmek icin).
 */
public class DepartmentInUseException extends RuntimeException {
    public DepartmentInUseException(String message) {
        super(message);
    }
}
