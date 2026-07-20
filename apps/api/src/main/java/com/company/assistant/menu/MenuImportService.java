package com.company.assistant.menu;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Akış:
 *  1) commit=false (varsayılan) -> sadece parse edilir, DB'ye dokunulmaz.
 *     Admin panel bu JSON'u ekranda gösterir ("İçe aktarılacaklar: 22 gün, 3 uyarı").
 *  2) Admin "Onayla ve Kaydet" derse, frontend AYNI dosyayı commit=true ile
 *     tekrar gönderir -> bu sefer veriler veritabanına yazılır.
 */
@Service
public class MenuImportService {

    private final MealMenuRepository mealMenuRepository;
    private final MealItemRepository mealItemRepository;

    public MenuImportService(MealMenuRepository mealMenuRepository,
                              MealItemRepository mealItemRepository) {
        this.mealMenuRepository = mealMenuRepository;
        this.mealItemRepository = mealItemRepository;
    }

    public MenuImportResponse importExcel(MultipartFile file, boolean commit) {
        validateFile(file);

        MenuExcelParser.ParseResult result;
        try (InputStream in = file.getInputStream()) {
            result = new MenuExcelParser().parse(in);
        } catch (IOException e) {
            throw new MenuImportException("Excel dosyası okunamadı: " + e.getMessage(), e);
        }

        if (result.days().isEmpty()) {
            throw new MenuImportException(
                    "Dosyada hiç tarih bulunamadı. Şablon değişmiş olabilir, "
                            + "lütfen yemekhaneden gelen dosyanın formatını kontrol et.");
        }

        if (commit) {
            saveToDatabase(result.days());
        }

        long daysWithNoData = result.days().stream().filter(d -> d.items().isEmpty()).count();

        return new MenuImportResponse(
                commit,
                result.days().size(),
                (int) daysWithNoData,
                result.days(),
                result.warnings()
        );
    }

    @Transactional
    void saveToDatabase(List<ParsedMenuDayDto> days) {
        for (ParsedMenuDayDto day : days) {
            if (day.items().isEmpty()) {
                continue; // veri yoksa o gün için hiçbir şey yazma
            }

            MealMenu menu = mealMenuRepository.findByDate(day.date())
                    .orElseGet(MealMenu::new);
            menu.setDate(day.date());
            menu.setWeekNumber(day.weekNumber());
            menu = mealMenuRepository.save(menu);

            mealItemRepository.deleteByMenuId(menu.getId());

            for (ParsedMealItemDto item : day.items()) {
                MealItem entity = new MealItem();
                entity.setMenu(menu);
                entity.setName(item.name());
                entity.setCategory(item.category());
                entity.setSortOrder(item.category().getSortOrder());
                // calories / allergens: bu şablonda gelmiyor -> null bırakılıyor
                mealItemRepository.save(entity);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MenuImportException("Dosya boş veya eksik.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !(name.endsWith(".xlsx") || name.endsWith(".xls"))) {
            throw new MenuImportException("Sadece .xlsx / .xls dosyaları kabul ediliyor.");
        }
    }
}