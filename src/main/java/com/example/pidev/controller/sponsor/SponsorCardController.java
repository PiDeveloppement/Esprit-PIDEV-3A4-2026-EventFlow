package com.example.pidev.controller.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

public class SponsorCardController {

    @FXML private ImageView logoView;
    @FXML private Label companyLabel;
    @FXML private Label emailLabel;
    @FXML private Label contributionLabel;
    @FXML private Label eventLabelPrefix;
    @FXML private Label eventLabel;
    @FXML private Button detailsBtn;
    @FXML private Button pdfBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private final SponsorService sponsorService = new SponsorService();

    public void setData(Sponsor sponsor, Runnable onDetails, Runnable onPdf, Runnable onEdit, Runnable onDelete) {
        if (sponsor == null) return;

        if (companyLabel != null) companyLabel.setText(nv(sponsor.getCompany_name()));
        if (emailLabel != null) emailLabel.setText(nv(sponsor.getContact_email()));
        if (contributionLabel != null) contributionLabel.setText(String.format("%,.2f DT", sponsor.getContribution_name()));

        if (eventLabel != null) {
            String title = null;
            try {
                title = sponsorService.getEventTitleById(sponsor.getEvent_id());
            } catch (Exception ignored) {
            }
            if (title == null || title.isBlank()) title = "-";
            eventLabel.setText(title);
        }

        if (logoView != null) {
            String logoUrl = sponsor.getLogo_url();
            if (logoUrl != null && !logoUrl.isBlank()) {
                try {
                    Image img = new Image(logoUrl, true);
                    img.errorProperty().addListener((obs, wasError, isError) -> {
                        if (Boolean.TRUE.equals(isError)) {
                            logoView.setImage(generateLocalLogoImage(sponsor));
                        }
                    });
                    logoView.setImage(img);
                } catch (Exception ignored) {
                    logoView.setImage(generateLocalLogoImage(sponsor));
                }
            } else {
                logoView.setImage(generateLocalLogoImage(sponsor));
            }
        }

        if (detailsBtn != null) detailsBtn.setOnAction(e -> { if (onDetails != null) onDetails.run(); });
        if (pdfBtn != null) pdfBtn.setOnAction(e -> { if (onPdf != null) onPdf.run(); });
        if (editBtn != null) editBtn.setOnAction(e -> { if (onEdit != null) onEdit.run(); });
        if (deleteBtn != null) deleteBtn.setOnAction(e -> { if (onDelete != null) onDelete.run(); });
    }

    private WritableImage generateLocalLogoImage(Sponsor sponsor) {
        String seed = nv(sponsor.getCompany_name()) + "|" + nv(sponsor.getContact_email());
        String initials = computeInitials(sponsor);

        int hash = Math.abs(seed.toLowerCase().hashCode());
        Color background = Color.hsb(hash % 360, 0.68, 0.74);

        Canvas canvas = new Canvas(96, 96);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(background);
        gc.fillRoundRect(0, 0, 96, 96, 18, 18);
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 34));
        gc.fillText(initials, 48, 52);

        WritableImage output = new WritableImage(96, 96);
        canvas.snapshot(new SnapshotParameters(), output);
        return output;
    }

    private String computeInitials(Sponsor sponsor) {
        String company = nv(sponsor.getCompany_name()).trim();
        if (!company.equals("-")) {
            String cleaned = company.replaceAll("[^A-Za-z0-9]", " ").trim();
            if (!cleaned.isBlank()) {
                String[] parts = cleaned.split("\\s+");
                if (parts.length >= 2) {
                    return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
                }
                String p = parts[0].toUpperCase();
                return p.length() >= 2 ? p.substring(0, 2) : p.substring(0, 1);
            }
        }

        String email = nv(sponsor.getContact_email());
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        local = local.replaceAll("[^A-Za-z0-9]", "");
        if (local.isBlank()) return "SP";
        local = local.toUpperCase();
        return local.length() >= 2 ? local.substring(0, 2) : local.substring(0, 1);
    }

    private static String nv(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
