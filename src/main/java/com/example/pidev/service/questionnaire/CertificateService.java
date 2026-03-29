package com.example.pidev.service.questionnaire;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CertificateService {

    public void genererCertificat(String nomParticipant, String score, String logoPath) {
        Document document = new Document(PageSize.A4.rotate());

        try {
            Path outputFile = buildOutputFilePath();
            PdfWriter.getInstance(document, new FileOutputStream(outputFile.toFile()));
            document.open();

            Image logo = resolveLogoImage(logoPath);
            if (logo != null) {
                logo.scaleToFit(150, 150);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
            }

            Font fontTitre = new Font(Font.FontFamily.HELVETICA, 45, Font.BOLD, new BaseColor(44, 62, 80));
            Paragraph pTitre = new Paragraph("CERTIFICAT DE REUSSITE", fontTitre);
            pTitre.setAlignment(Element.ALIGN_CENTER);
            pTitre.setSpacingBefore(20);
            document.add(pTitre);

            Font fontSousTitre = new Font(Font.FontFamily.HELVETICA, 20, Font.ITALIC, BaseColor.GRAY);
            Paragraph pSousTitre = new Paragraph("\nCe certificat est fierement decerne a :", fontSousTitre);
            pSousTitre.setAlignment(Element.ALIGN_CENTER);
            document.add(pSousTitre);

            Font fontNom = new Font(Font.FontFamily.TIMES_ROMAN, 35, Font.BOLD, new BaseColor(41, 128, 185));
            Paragraph pNom = new Paragraph(nomParticipant, fontNom);
            pNom.setAlignment(Element.ALIGN_CENTER);
            pNom.setSpacingBefore(15);
            document.add(pNom);

            Font fontDetail = new Font(Font.FontFamily.HELVETICA, 18, Font.NORMAL);
            Paragraph pDetail = new Paragraph(
                    "\nPour avoir brillamment reussi le quiz de l'evenement\navec un score exceptionnel de :",
                    fontDetail
            );
            pDetail.setAlignment(Element.ALIGN_CENTER);
            document.add(pDetail);

            Font fontScore = new Font(Font.FontFamily.HELVETICA, 30, Font.BOLD, new BaseColor(39, 174, 96));
            Paragraph pScore = new Paragraph(score, fontScore);
            pScore.setAlignment(Element.ALIGN_CENTER);
            pScore.setSpacingBefore(10);
            document.add(pScore);

            String dateDuJour = new SimpleDateFormat("dd MMMM yyyy").format(new Date());
            Font fontDate = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC);
            Paragraph pDate = new Paragraph("\nFait le " + dateDuJour, fontDate);
            pDate.setAlignment(Element.ALIGN_CENTER);
            pDate.setSpacingBefore(30);
            document.add(pDate);

            document.close();
            System.out.println("Certificat genere avec succes: " + outputFile.toAbsolutePath());
        } catch (Exception e) {
            if (document.isOpen()) {
                document.close();
            }
            System.err.println("Erreur generation certificat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Path buildOutputFilePath() throws Exception {
        Path outputDir = Paths.get(System.getProperty("user.dir"), "target", "certificates");
        Files.createDirectories(outputDir);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return outputDir.resolve("Certificat_EventFlow_" + timestamp + ".pdf");
    }

    private Image resolveLogoImage(String logoPath) {
        try {
            if (logoPath != null && !logoPath.isBlank()) {
                // Absolute or relative filesystem path.
                File asFile = new File(logoPath);
                if (asFile.exists()) {
                    return Image.getInstance(asFile.getAbsolutePath());
                }

                // URL like file:/... or http(s)://...
                if (logoPath.startsWith("file:") || logoPath.startsWith("http://") || logoPath.startsWith("https://")) {
                    return Image.getInstance(logoPath);
                }

                // Classpath resource path.
                String normalized = logoPath.startsWith("/") ? logoPath : "/" + logoPath;
                try (InputStream in = CertificateService.class.getResourceAsStream(normalized)) {
                    if (in != null) {
                        return Image.getInstance(in.readAllBytes());
                    }
                }
            }

            // Fallback default logo in resources.
            try (InputStream fallback = CertificateService.class.getResourceAsStream("/com/example/pidev/images/logo.png")) {
                if (fallback != null) {
                    return Image.getInstance(fallback.readAllBytes());
                }
            }
        } catch (Exception e) {
            System.err.println("Logo non charge: " + e.getMessage());
        }
        return null;
    }
}
