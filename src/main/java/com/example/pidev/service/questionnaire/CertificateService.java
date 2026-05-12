package com.example.pidev.service.questionnaire;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.PageSize;  // IMPORTANT: utiliser PageSize d'iText 5, pas celui d'iText 7

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CertificateService {

    /**
     * Génère un certificat de réussite au format PDF
     * @param nomParticipant Le nom de l'utilisateur
     * @param score Le score obtenu (ex: "8/10")
     * @param logoPath Le chemin vers l'image du logo EventFlow
     */
    public void genererCertificat(String nomParticipant, String score, String logoPath) {
        // Document en format A4 et en mode Paysage (Landscape) - Version iText 5
        Document document = new Document(PageSize.A4.rotate());

        try {
            PdfWriter.getInstance(document, new FileOutputStream("Certificat_EventFlow.pdf"));
            document.open();

            // 1. Chargement et ajout du Logo
            if (logoPath != null && !logoPath.isEmpty()) {
                try {
                    Image logo = Image.getInstance(logoPath);
                    logo.scaleToFit(150, 150);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                } catch (Exception e) {
                    System.out.println("Logo non trouvé: " + logoPath);
                }
            }

            // 2. Titre Principal
            Font fontTitre = new Font(Font.FontFamily.HELVETICA, 45, Font.BOLD, new BaseColor(44, 62, 80));
            Paragraph pTitre = new Paragraph("CERTIFICAT DE RÉUSSITE", fontTitre);
            pTitre.setAlignment(Element.ALIGN_CENTER);
            pTitre.setSpacingBefore(20);
            document.add(pTitre);

            // 3. Texte de présentation
            Font fontSousTitre = new Font(Font.FontFamily.HELVETICA, 20, Font.ITALIC, BaseColor.GRAY);
            Paragraph pSousTitre = new Paragraph("\nCe certificat est fièrement décerné à :", fontSousTitre);
            pSousTitre.setAlignment(Element.ALIGN_CENTER);
            document.add(pSousTitre);

            // 4. Nom du Gagnant
            Font fontNom = new Font(Font.FontFamily.TIMES_ROMAN, 35, Font.BOLD, new BaseColor(41, 128, 185));
            Paragraph pNom = new Paragraph(nomParticipant, fontNom);
            pNom.setAlignment(Element.ALIGN_CENTER);
            pNom.setSpacingBefore(15);
            document.add(pNom);

            // 5. Détails du succès
            Font fontDetail = new Font(Font.FontFamily.HELVETICA, 18, Font.NORMAL);
            Paragraph pDetail = new Paragraph("\nPour avoir brillamment réussi le quiz de l'événement\navec un score exceptionnel de :", fontDetail);
            pDetail.setAlignment(Element.ALIGN_CENTER);
            document.add(pDetail);

            // 6. Affichage du Score
            Font fontScore = new Font(Font.FontFamily.HELVETICA, 30, Font.BOLD, new BaseColor(39, 174, 96));
            Paragraph pScore = new Paragraph(score, fontScore);
            pScore.setAlignment(Element.ALIGN_CENTER);
            pScore.setSpacingBefore(10);
            document.add(pScore);

            // 7. Date du jour
            String dateDuJour = new SimpleDateFormat("dd MMMM yyyy").format(new Date());
            Font fontDate = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC);
            Paragraph pDate = new Paragraph("\nFait le " + dateDuJour, fontDate);
            pDate.setAlignment(Element.ALIGN_CENTER);
            pDate.setSpacingBefore(30);
            document.add(pDate);

            document.close();
            System.out.println("✅ Certificat généré avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération du certificat:");
            e.printStackTrace();
        }
    }
}