package com.company.assistant.menu;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * C-2: POST /admin/menus/import, DELETE /admin/menus/{id}
 * #194: PUT /admin/menus/{id} - bir gunun kalemlerini (kategori + isim) Excel'i
 * yeniden yuklemeden elle duzenleme.
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
    private final MealItemRepository mealItemRepository;

    public AdminMenuController(MenuImportService menuImportService,
                                MealMenuRepository mealMenuRepository,
                                MealItemRepository mealItemRepository) {
        this.menuImportService = menuImportService;
        this.mealMenuRepository = mealMenuRepository;
        this.mealItemRepository = mealItemRepository;
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

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<MenuResponse> updateMenu(@PathVariable Integer id, @Valid @RequestBody UpdateMenuRequest request) {
        MealMenu menu = mealMenuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menü bulunamadı: id=" + id));

        mealItemRepository.deleteByMenuId(id);

        List<MealItem> items = request.items().stream().map(itemRequest -> {
            MealItem entity = new MealItem();
            entity.setMenu(menu);
            entity.setName(itemRequest.name().trim());
            entity.setCategory(itemRequest.category());
            entity.setSortOrder(itemRequest.category().getSortOrder());
            return entity;
        }).toList();

        menu.setItems(mealItemRepository.saveAll(items));
        return ResponseEntity.ok(new MenuResponse(menu));
    }

    @ExceptionHandler(MenuImportException.class)
    public ResponseEntity<String> handleImportError(MenuImportException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getMessage());
    }
}