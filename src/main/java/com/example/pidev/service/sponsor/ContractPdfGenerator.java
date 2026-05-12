package com.example.pidev.service.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class ContractPdfGenerator {

    // mets ton logo ici:
    // src/main/resources/com/example/pidev/icons/eventflow_logo.png
    private static final String APP_LOGO_CLASSPATH = "/com/example/pidev/icons/eventflow_logo.png";

    public static File generate(Sponsor s) {
        try {
            File out = Files.createTempFile("contract_sponsor_" + safeId(s) + "_", ".pdf").toFile();
            out.deleteOnExit();

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float margin = 50;
                    float y = page.getMediaBox().getHeight() - margin;

                    // ---- logo app ----
                    try (InputStream is = ContractPdfGenerator.class.getResourceAsStream(APP_LOGO_CLASSPATH)) {
                        if (is != null) {
                            byte[] logoBytes = is.readAllBytes();
                            PDImageXObject logo = PDImageXObject.createFromByteArray(doc, logoBytes, "eventflow_logo");
                            float w = 80, h = 80;
                            cs.drawImage(logo, margin, y - h, w, h);
                        }
                    } catch (Exception ignored) {}

                    // ---- titre ----
                    text(cs, margin + 95, y - 35, PDType1Font.HELVETICA_BOLD, 18, "Contrat de Sponsoring - EventFlow");
                    y -= 120;

                    // ---- infos ----
                    y = line(cs, margin, y, true, 12, "Entreprise : " + safe(s.getCompany_name()));
                    y = line(cs, margin, y, false, 12, "Email : " + safe(s.getContact_email()));
                    y = line(cs, margin, y, false, 12, "Event ID : " + s.getEvent_id());
                    y = line(cs, margin, y, false, 12, "Contribution : " + String.format("%,.2f DT", s.getContribution_name()));
                    y -= 10;

                    y = line(cs, margin, y, true, 12, "Objet :");
                    y = paragraph(cs, margin, y,
                            "Le présent contrat formalise l'engagement de l'entreprise sponsor à contribuer au financement "
                                    + "de l'événement indiqué ci-dessus. En contrepartie, EventFlow assurera la visibilité du sponsor "
                                    + "conformément aux modalités convenues.",
                            12);

                    y -= 10;
                    y = line(cs, margin, y, true, 12, "Référence logo sponsor (Cloudinary) :");
                    y = line(cs, margin, y, false, 11, safe(s.getLogo_url()));
                    y -= 25;

                    // ---- signatures ----
                    y = line(cs, margin, y, true, 12, "Signature sponsor :");
                    rect(cs, margin, y - 80, 240, 80);
                    y -= 100;

                    y = line(cs, margin, y, true, 12, "Signature EventFlow :");
                    rect(cs, margin, y - 80, 240, 80);

                    // zone logo/cachet sponsor
                    rect(cs, 330, page.getMediaBox().getHeight() - 260, 200, 140);
                    text(cs, 338, page.getMediaBox().getHeight() - 260 + 125, PDType1Font.HELVETICA, 10, "Zone Logo / Cachet Sponsor");
                }

                doc.save(out);
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private static void text(PDPageContentStream cs, float x, float y,
                             org.apache.pdfbox.pdmodel.font.PDFont font, int size, String txt) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(txt == null ? "" : txt);
        cs.endText();
    }

    private static float line(PDPageContentStream cs, float x, float y, boolean bold, int size, String txt) throws Exception {
        text(cs, x, y, bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, size, txt);
        return y - 18;
    }

    private static float paragraph(PDPageContentStream cs, float x, float y, String text, int size) throws Exception {
        String[] lines = wrap(text, 92);
        for (String l : lines) y = line(cs, x, y, false, size, l);
        return y;
    }

    private static String[] wrap(String text, int maxChars) {
        if (text == null) return new String[]{""};
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split("\\s+")) {
            if (cur.length() + w.length() + 1 > maxChars) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(" ");
            cur.append(w);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private static void rect(PDPageContentStream cs, float x, float y, float w, float h) throws Exception {
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static int safeId(Sponsor s) { return s == null ? 0 : s.getId(); }
}
