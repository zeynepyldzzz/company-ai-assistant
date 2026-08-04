package com.company.assistant.menu;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * C-2: POST /admin/menus/import, DELETE /admin/menus/{id}
 *
 * C-14 (#123): once sadece genel hasRole("ADMIN") kontrolu vardi (hangi
 * sub-role oldugu onemsizdi, ornegin bir hr_admin de menu yukleyebiliyordu).
 * Diger admin modulleriyle tutarli olmasi icin canteen_admin ve system_admin
 * ile sinirlandirildi.
 */
@RestController
@RequestMapping("/admin/menus")
@PreAuthorize("hasAuthority('ROLE_CANTEEN_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminMenuController {

    private final MenuImportService menuImportService;
    private final MealMenuRepository mealMenuRepository;

    public AdminMenuController(MenuImportService menuImportService,
                                MealMenuRepository mealMenuRepository) {
        this.menuImportService = menuImportService;
        this.mealMenuRepository = mealMenuRepository;
    }

    /**
     * commit=false (varsayılan) -> sadece parse edip önizleme döner, DB'ye yazmaz.
     * commit=true               -> parse edip veritabanına kaydeder.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuImportResponse> importMenu(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "commit", defaultValue = "false") boolean commit) {
        MenuImportResponse response = menuImportService.importExcel(file, commit);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Integer id) {
        if (!mealMenuRepository.existsById(id)) {
            throw new EntityNotFoundException("Menu bulunamadı: id=" + id);
        }
        mealMenuRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MenuImportException.class)
    public ResponseEntity<String> handleImportError(MenuImportException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }
}