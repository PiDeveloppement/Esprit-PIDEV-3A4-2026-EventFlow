package com.example.pidev.service.pdf;

import com.example.pidev.model.sponsor.Sponsor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LocalSponsorPdfService {

    private final File baseDir;
    private final HttpClient http = HttpClient.newHttpClient();

    public LocalSponsorPdfService() {
        this.baseDir = new File(System.getProperty("user.home"), "EventFlow_PDF");
    }

    /**
     * PDF local “Contrat” basé sur ce qui s'affiche dans Details,
     * MAIS sans afficher IDs, sans URL logo/contrat, et avec logos + cachet.
     */
    public File generateSponsorContractPdf(Sponsor s, String eventTitle) throws Exception {
        if (s == null) throw new IllegalArgumentException("Sponsor null");

        File outDir = new File(baseDir, "sponsors");
        if (!outDir.exists()) Files.createDirectories(outDir.toPath());

        String safeCompany = safeFilePart(nv(s.getCompany_name()));
        File outFile = new File(outDir, "CONTRAT_SPONSOR_" + safeCompany + ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50f;
                float pageW = page.getMediaBox().getWidth();
                float pageH = page.getMediaBox().getHeight();
                float usableW = pageW - 2 * margin;

                float y = pageH - margin;

                // ====== LOGOS (EventFlow à gauche, Entreprise à droite) ======
                float logoBoxH = 52f;
                float logoBoxW = 52f;

                // EventFlow logo (classpath) : /com/example/pidev/icons/logo.png
                PDImageXObject eventflowLogo = loadClasspathImage(doc, "/com/example/pidev/icons/logo.png");
                if (eventflowLogo != null) {
                    cs.drawImage(eventflowLogo, margin, y - logoBoxH, logoBoxW, logoBoxH);
                }

                // Company logo (depuis sponsor.logo_url) => on l'EMBED (pas afficher URL)
                PDImageXObject companyLogo = loadCompanyLogo(doc, s.getLogo_url());
                if (companyLogo != null) {
                    cs.drawImage(companyLogo, pageW - margin - 78f, y - 58f, 78f, 58f);
                }

                // Descendre sous les logos
                y -= (logoBoxH + 16f);

                // ====== TITRE ======
                y = line(cs, "DETAILS DU SPONSOR", margin, y, PDType1Font.HELVETICA_BOLD, 18);
                y -= 6;

                // ====== DATE ======
                String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                y = line(cs, "Date: " + date, margin, y, PDType1Font.HELVETICA, 11);
                y -= 8;

                // ====== EVENEMENT (sans ID) ======
                String ev = (eventTitle == null || eventTitle.isBlank()) ? "—" : eventTitle;
                y = line(cs, "Evenement: " + ev, margin, y, PDType1Font.HELVETICA, 11);
                y -= 12;

                // ====== INFOS (comme Details, sans URLs) ======
                y = line(cs, "Informations:", margin, y, PDType1Font.HELVETICA_BOLD, 12);
                y -= 2;

                y = line(cs, "Entreprise: " + nv(s.getCompany_name()), margin, y, PDType1Font.HELVETICA, 11);
                y = line(cs, "Email: " + nv(s.getContact_email()), margin, y, PDType1Font.HELVETICA, 11);
                y = line(cs, "Contribution: " + fmt(s.getContribution_name()) + " DT", margin, y, PDType1Font.HELVETICA, 11);

                y -= 16;

                // ====== CLAUSES SIMPLE ======
                y = line(cs, "Contrat Sponsor:", margin, y, PDType1Font.HELVETICA_BOLD, 12);
                y -= 4;

                y = paragraph(cs,
                        "Le sponsor confirme sa contribution a l'evenement mentionne ci-dessus. "
                                + "La contribution sera utilisee pour les besoins organisationnels de l'evenement.",
                        margin, y, usableW, PDType1Font.HELVETICA, 11, 14);

                y -= 12;
                // ====== SIGNATURES (gauche/droite, sans ligne) ======
                float sigY = y;

// labels sur la même ligne
                drawAt(cs, "Signature Organisateur:", margin, sigY, PDType1Font.HELVETICA, 11);
                drawAt(cs, "Signature Sponsor:", margin + (usableW / 2f) + 20f, sigY, PDType1Font.HELVETICA, 11);

// espace vide pour signer (pas de trait)
                y = sigY - 60f;




                // ====== CACHET OFFICIEL (dessin) ======
                drawOfficialStamp(cs, pageW - margin - 180f, margin + 70f, 180f, 70f, date);
            }

            doc.save(outFile);
        }

        return outFile;
    }

    // ===================== CACHET =====================
    private static void drawOfficialStamp(PDPageContentStream cs, float x, float y, float w, float h, String date) throws Exception {
        cs.setLineWidth(2f);
        cs.addRect(x, y, w, h);
        cs.stroke();

        float ty = y + h - 18f;
        ty = text(cs, "CACHET OFFICIEL", x + 14f, ty, PDType1Font.HELVETICA_BOLD, 12);
        ty = text(cs, "EVENTFLOW", x + 14f, ty, PDType1Font.HELVETICA_BOLD, 12);
        text(cs, "Date: " + date, x + 14f, y + 14f, PDType1Font.HELVETICA, 10);
    }

    // ===================== IMAGES =====================
    private PDImageXObject loadClasspathImage(PDDocument doc, String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            byte[] bytes = readAllBytes(is);
            return PDImageXObject.createFromByteArray(doc, bytes, "eventflow_logo");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Logo entreprise: on EMBED l'image.
     * - si logo_url est un chemin local -> lire fichier
     * - si logo_url est http(s) -> télécharger l'image (sans afficher URL dans PDF)
     * - sinon -> null
     */
    private PDImageXObject loadCompanyLogo(PDDocument doc, String logoUrlOrPath) {
        try {
            if (logoUrlOrPath == null || logoUrlOrPath.isBlank()) return null;

            byte[] imgBytes = null;

            String v = logoUrlOrPath.trim();
            if (v.startsWith("http://") || v.startsWith("https://")) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(v))
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    imgBytes = resp.body();
                }
            } else {
                File f = new File(v);
                if (f.exists() && f.isFile()) imgBytes = Files.readAllBytes(f.toPath());
            }

            if (imgBytes == null || imgBytes.length == 0) return null;
            return PDImageXObject.createFromByteArray(doc, imgBytes, "company_logo");
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
        return bos.toByteArray();
    }

    // ===================== TEXTE / WRAP =====================
    private static float line(PDPageContentStream cs, String text, float x, float y,
                              PDType1Font font, int size) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y - (size + 6);
    }

    private static float text(PDPageContentStream cs, String text, float x, float y,
                              PDType1Font font, int size) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
        return y - (size + 6);
    }

    private static float paragraph(PDPageContentStream cs, String text, float x, float y, float width,
                                   PDType1Font font, int size, float leading) throws Exception {
        List<String> lines = wrap(text == null ? "" : text.replace("\r", ""), width, font, size);

        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        for (String l : lines) {
            cs.showText(l);
            cs.newLineAtOffset(0, -leading);
        }
        cs.endText();

        return y - leading * lines.size();
    }

    private static List<String> wrap(String text, float width, PDType1Font font, int size) throws Exception {
        String[] words = text.trim().split("\\s+");
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = line.length() == 0 ? w : line + " " + w;
            float wWidth = font.getStringWidth(candidate) / 1000f * size;

            if (wWidth > width && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    // ===================== UTILS =====================
    private static String nv(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String fmt(double v) {
        return String.format("%,.2f", v);
    }
    private static void drawAt(PDPageContentStream cs, String text, float x, float y,
                               PDType1Font font, int size) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }



    private static String safeFilePart(String s) {
        String v = (s == null || s.isBlank()) ? "SPONSOR" : s;
        v = v.replaceAll("[\\\\/:*?\"<>|]", "_");
        v = v.replaceAll("\\s+", "_");
        if (v.length() > 30) v = v.substring(0, 30);
        return v;
    }
}
