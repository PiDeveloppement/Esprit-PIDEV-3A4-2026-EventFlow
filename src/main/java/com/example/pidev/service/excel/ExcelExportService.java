package com.example.pidev.service.excel;

import com.example.pidev.model.depense.Depense;
import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.budget.BudgetService;
import com.example.pidev.service.chart.QuickChartService;
import com.example.pidev.service.sponsor.SponsorService;
import com.google.gson.JsonObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SponsorService sponsorService = new SponsorService();
    private static final BudgetService budgetService = new BudgetService();

    /**
     * Exporte la liste des sponsors vers un fichier Excel avec un graphique.
     * @param sponsors liste des sponsors
     * @param chartConfig configuration JSON du graphique (ex: répartition des contributions)
     * @param filePath chemin de sauvegarde du fichier
     */
    public static void exportSponsors(List<Sponsor> sponsors, JsonObject chartConfig, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet dataSheet = workbook.createSheet("Sponsors");
            createSponsorsSheet(dataSheet, sponsors);
            Sheet chartSheet = workbook.createSheet("Graphique");
            addChartToSheet(chartSheet, chartConfig, "Répartition des contributions");
            // Ajuster la largeur des colonnes
            for (int i = 0; i < 5; i++) {
                dataSheet.autoSizeColumn(i);
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Exporte la liste des dépenses vers un fichier Excel avec un graphique.
     * @param depenses liste des dépenses
     * @param chartConfig configuration JSON du graphique
     * @param filePath chemin de sauvegarde
     */
    public static void exportDepenses(List<Depense> depenses, JsonObject chartConfig, String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet dataSheet = workbook.createSheet("Dépenses");
            createDepensesSheet(dataSheet, depenses);
            Sheet chartSheet = workbook.createSheet("Graphique");
            addChartToSheet(chartSheet, chartConfig, "Répartition des dépenses par catégorie");
            for (int i = 0; i < 6; i++) {
                dataSheet.autoSizeColumn(i);
            }
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    private static void createSponsorsSheet(Sheet sheet, List<Sponsor> sponsors) {
        // Créer l'en-tête
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Entreprise", "Email", "Contribution (DT)", "Événement"};
        CellStyle headerStyle = getHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Remplir les données
        int rowNum = 1;
        for (Sponsor s : sponsors) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(s.getId());
            row.createCell(1).setCellValue(s.getCompany_name());
            row.createCell(2).setCellValue(s.getContact_email());
            row.createCell(3).setCellValue(s.getContribution_name());

            // Récupérer le titre de l'événement via SponsorService
            String eventTitle = "—";
            try {
                eventTitle = sponsorService.getEventTitleById(s.getEvent_id());
            } catch (Exception e) {
                eventTitle = "Erreur";
            }
            row.createCell(4).setCellValue(eventTitle);
        }
    }

    private static void createDepensesSheet(Sheet sheet, List<Depense> depenses) {
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Budget", "Description", "Montant (DT)", "Catégorie", "Date"};
        CellStyle headerStyle = getHeaderStyle(sheet.getWorkbook());
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Depense d : depenses) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(d.getId());

            // Récupérer le nom du budget via BudgetService
            String budgetName = "—";
            try {
                budgetName = budgetService.getBudgetNameById(d.getBudget_id());
            } catch (Exception e) {
                budgetName = "Budget " + d.getBudget_id();
            }
            row.createCell(1).setCellValue(budgetName);

            row.createCell(2).setCellValue(d.getDescription());
            row.createCell(3).setCellValue(d.getAmount());
            row.createCell(4).setCellValue(d.getCategory());
            row.createCell(5).setCellValue(d.getExpense_date() != null ? d.getExpense_date().format(DATE_FMT) : "");
        }
    }

    private static CellStyle getHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Ajoute une image (graphique) dans une feuille Excel à partir d'une configuration QuickChart.
     */
    private static void addChartToSheet(Sheet sheet, JsonObject chartConfig, String title) throws IOException {
        // Générer l'URL du graphique
        String chartUrl = QuickChartService.getChartUrl(chartConfig);

        // Télécharger l'image
        BufferedImage image = ImageIO.read(new URL(chartUrl));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Ajouter l'image au classeur
        int pictureIdx = sheet.getWorkbook().addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = sheet.getWorkbook().getCreationHelper().createClientAnchor();
        anchor.setCol1(0);
        anchor.setRow1(1);
        anchor.setCol2(8); // largeur de l'image
        anchor.setRow2(25); // hauteur
        Picture pict = drawing.createPicture(anchor, pictureIdx);
        pict.resize(); // redimensionne automatiquement

        // Ajouter un titre
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
    }
}