package com.example.pidev.controller.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.service.pdf.LocalSponsorPdfService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

public class SponsorDetailsController {

    @FXML private Label companyLabel;
    @FXML private Label emailLabel;
    @FXML private Label contributionLabel;
    @FXML private Label logoUrlLabel;
    @FXML private Label contractUrlLabel;
    @FXML private ImageView sponsorLogoView;
    @FXML private Button openContractBtn;
    @FXML private Button okBtn;
    @FXML private Label taxIdLabel;
    @FXML private Hyperlink documentLink;

    private Sponsor sponsor;
    private Runnable onBack;

    public void setSponsor(Sponsor sponsor) {
        this.sponsor = sponsor;

        if (sponsor == null) return;

        if (companyLabel != null) companyLabel.setText(nvl(sponsor.getCompany_name()));
        if (emailLabel != null) emailLabel.setText(nvl(sponsor.getContact_email()));
        if (contributionLabel != null) contributionLabel.setText(String.format("%,.2f DT", sponsor.getContribution_name()));
        if (logoUrlLabel != null) logoUrlLabel.setText(nvl(sponsor.getLogo_url()));
        if (contractUrlLabel != null) contractUrlLabel.setText(nvl(sponsor.getContract_url()));
        if (taxIdLabel != null) taxIdLabel.setText(nvl(sponsor.getTax_id()));

        // Gestion du lien justificatif
        if (documentLink != null) {
            String docUrl = sponsor.getDocument_url();
            if (docUrl != null && !docUrl.isBlank()) {
                documentLink.setText("Voir le justificatif");
                documentLink.setDisable(false);
                documentLink.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().browse(URI.create(docUrl));
                    } catch (Exception ex) {
                        showAlert("Erreur", "Impossible d'ouvrir le justificatif : " + ex.getMessage());
                    }
                });
            } else {
                documentLink.setText("Aucun justificatif");
                documentLink.setDisable(true);
            }
        }

        try {
            if (sponsorLogoView != null && sponsor.getLogo_url() != null && !sponsor.getLogo_url().isBlank()) {
                sponsorLogoView.setImage(new Image(sponsor.getLogo_url(), true));
            }
        } catch (Exception ignored) {}

        // Le bouton contrat est toujours actif (on génère localement si pas d'URL)
        if (openContractBtn != null) {
            openContractBtn.setDisable(false);
        }
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void onOpenContract() {
        try {
            if (sponsor == null) return;

            // 1. Si une URL de contrat existe, l'ouvrir dans le navigateur
            String url = sponsor.getContract_url();
            if (url != null && !url.isBlank()) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }

            // 2. Sinon, générer un contrat local et l'ouvrir
            SponsorService sponsorService = new SponsorService();
            String eventTitle = sponsorService.getEventTitleById(sponsor.getEvent_id());
            File pdf = new LocalSponsorPdfService().generateSponsorContractPdf(sponsor, eventTitle);
            if (pdf.exists() && pdf.length() > 0) {
                Desktop.getDesktop().open(pdf);
            } else {
                showAlert("Erreur", "Le fichier PDF n'a pas pu être généré.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le contrat : " + e.getMessage());
        }
    }

    @FXML
    private void onOk() {
        if (onBack != null) onBack.run();
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}