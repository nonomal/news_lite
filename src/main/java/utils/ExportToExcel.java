package utils;

import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class ExportToExcel {
    private static final String[] HEADERS = {"Number", "Source", "Title", "Date", "Link"};
    private static final String SHEET_FONT = "Arial";
    private static final short SHEET_FONT_SIZE = (short) 13;
    private static final short SHEET_ROWS_HEIGHT =  (short) 400;
    private final Workbook workbook = new HSSFWorkbook();
    private final Sheet sheet = workbook.createSheet("Avandy-news");

    public void exportResultsToExcel() {
        try {
            // Save file to
            FileNameExtensionFilter filter = new FileNameExtensionFilter("*.xls",
                    "*.xls", "*.XLS", "*.*");
            JFileChooser saveToDirectory = new JFileChooser();
            saveToDirectory.setFileFilter(filter);
            saveToDirectory.setCurrentDirectory(new File
                    (System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop"));
            int ret = saveToDirectory.showDialog(null, "Save");
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = new File(saveToDirectory.getSelectedFile() + ".xls");

                sheet.setColumnWidth(0, 3000);
                sheet.setColumnWidth(1, 4000);
                sheet.setColumnWidth(2, 30000);
                sheet.setColumnWidth(3, 4000);
                sheet.setColumnWidth(4, 3000);

                // Headers
                Row headerRow = sheet.createRow(0);
                headerRow.setHeight(SHEET_ROWS_HEIGHT);

                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);

                Font headersFont = workbook.createFont();
                headersFont.setFontName(SHEET_FONT);
                headersFont.setFontHeightInPoints(SHEET_FONT_SIZE);
                headersFont.setBold(true);
                headerStyle.setFont(headersFont);

                // Cells
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                // Sources, Titles
                CellStyle leftCellStyle = workbook.createCellStyle();
                leftCellStyle.setBorderLeft(BorderStyle.THIN);
                leftCellStyle.setBorderRight(BorderStyle.THIN);
                leftCellStyle.setBorderBottom(BorderStyle.THIN);
                leftCellStyle.setAlignment(HorizontalAlignment.LEFT);
                leftCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                Font cellFont = workbook.createFont();
                cellFont.setFontName(SHEET_FONT);
                cellFont.setFontHeightInPoints(SHEET_FONT_SIZE);

                cellStyle.setFont(cellFont);
                leftCellStyle.setFont(cellFont);

                for (int i = 0; i < HEADERS.length; i++) {
                    Cell header = headerRow.createCell(i);
                    header.setCellValue(HEADERS[i]);
                    header.setCellStyle(headerStyle);
                }

                for (int i = 0; i < Gui.model.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 1);
                    row.setHeight(SHEET_ROWS_HEIGHT);

                    // "Number"
                    Cell number = row.createCell(0);
                    number.setCellValue((Integer) Gui.model.getValueAt(i, 0));
                    number.setCellStyle(cellStyle);

                    // "Source"
                    Cell source = row.createCell(1);
                    source.setCellValue(Gui.model.getValueAt(i, 1).toString());
                    source.setCellStyle(leftCellStyle);

                    // "Title"
                    Cell title = row.createCell(2);
                    title.setCellValue(Gui.model.getValueAt(i, 2).toString());
                    title.setCellStyle(leftCellStyle);

                    // "Date"
                    Cell date = row.createCell(3);
                    date.setCellValue(Gui.model.getValueAt(i, 3).toString());
                    date.setCellStyle(cellStyle);

                    // "Link"
                    Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    hyperlink.setAddress(Gui.model.getValueAt(i, 4).toString());
                    Cell link = row.createCell(4);
                    link.setCellValue("⟶");
                    link.setHyperlink(hyperlink);
                    link.setCellStyle(cellStyle);
                }

                // write to file
                workbook.write(Files.newOutputStream(file.toPath()));
                workbook.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
