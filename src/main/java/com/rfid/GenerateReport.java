package com.rfid;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class GenerateReport {

    private final static String HEADERS[] = {"tagId","firstSeen","lastSeen","antenna"};

    public ByteArrayInputStream generateCustomerReport(List<TagDetail> tags) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String sheetName = "tags";
            Sheet sheet = workbook.createSheet(sheetName);

            // Header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(HEADERS[i]);
            }
            generateSheetCells(tags, sheet);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void generateSheetCells(List<TagDetail> customers, Sheet sheet) {
        int rowIndex = 1;
        for (TagDetail c : customers) {
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(c.getTagId());
            dataRow.createCell(1).setCellValue(c.getFirstSeen().toString());
            dataRow.createCell(2).setCellValue(c.getLastSeen().toString());
            dataRow.createCell(3).setCellValue(c.getAntenna());
        }
    }
}