package com.company.assistant.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

// C-11 (#85): usage raporunu xlsx olarak disa aktarma (POI zaten menu import icin projede mevcut).
@Service
public class ReportExportService {

    public byte[] toXlsx(UsageReportResponse report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(report.type());

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Modul");
            header.createCell(1).setCellValue("Metrik");
            header.createCell(2).setCellValue("Sayi");

            int rowIndex = 1;
            for (UsageReportRow row : report.rows()) {
                Row excelRow = sheet.createRow(rowIndex++);
                excelRow.createCell(0).setCellValue(row.module());
                excelRow.createCell(1).setCellValue(row.metric());
                Cell countCell = excelRow.createCell(2);
                countCell.setCellValue(row.count());
            }

            for (int col = 0; col < 3; col++) {
                sheet.autoSizeColumn(col);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Rapor xlsx olusturulamadi", e);
        }
    }
}
