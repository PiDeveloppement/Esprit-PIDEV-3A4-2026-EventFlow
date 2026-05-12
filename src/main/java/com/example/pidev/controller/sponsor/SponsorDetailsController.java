package com.example.pidev.controller.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
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

        try {
            if (sponsorLogoView != null && sponsor.getLogo_url() != null && !sponsor.getLogo_url().isBlank()) {
                sponsorLogoView.setImage(new Image(sponsor.getLogo_url(), true));
            }
        } catch (Exception ignored) {}

        if (openContractBtn != null) {
            boolean has = sponsor.getContract_url() != null && !sponsor.getContract_url().isBlank();
            openContractBtn.setDisable(!has);
        }
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    @FXML
    private void onOpenContract() {
        try {
            if (sponsor == null) return;
            String url = sponsor.getContract_url();
            if (url == null || url.isBlank()) return;
            if (!Desktop.isDesktopSupported()) return;
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {}
    }

    @FXML
    private void onOk() {
        if (onBack != null) onBack.run();
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

}