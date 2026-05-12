package com.example.pidev.controller.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SponsorCardController {

    @FXML private ImageView logoView;
    @FXML private Label companyLabel;
    @FXML private Label emailLabel;
    @FXML private Label contributionLabel;
    @FXML private Label eventLabel;
    @FXML private Button detailsBtn;
    @FXML private Button pdfBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private final SponsorService sponsorService = new SponsorService();

    public void setData(Sponsor s, Runnable onDetails, Runnable onPdf, Runnable onEdit, Runnable onDelete) {
        if (s == null) return;

        if (companyLabel != null) companyLabel.setText(nv(s.getCompany_name()));
        if (emailLabel != null) emailLabel.setText(nv(s.getContact_email()));
        if (contributionLabel != null) contributionLabel.setText(String.format("%,.2f DT", s.getContribution_name()));

        if (eventLabel != null) {
            String title = sponsorService.getEventTitleById(s.getEvent_id());
            if (title == null || title.isBlank()) title = "—";
            eventLabel.setText("Événement: " + title);
        }

        try {
            if (logoView != null && s.getLogo_url() != null && !s.getLogo_url().isBlank()) {
                logoView.setImage(new Image(s.getLogo_url(), true));
            } else if (logoView != null) {
                logoView.setImage(null);
            }
        } catch (Exception ignored) {}

        if (detailsBtn != null) detailsBtn.setOnAction(e -> { if (onDetails != null) onDetails.run(); });
        if (pdfBtn != null) pdfBtn.setOnAction(e -> { if (onPdf != null) onPdf.run(); });
        if (editBtn != null) editBtn.setOnAction(e -> { if (onEdit != null) onEdit.run(); });
        if (deleteBtn != null) deleteBtn.setOnAction(e -> { if (onDelete != null) onDelete.run(); });
    }

    private static String nv(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}