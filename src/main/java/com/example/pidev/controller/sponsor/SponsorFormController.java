package com.example.pidev.controller.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.service.upload.CloudinaryUploadService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SponsorFormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> eventComboBox;
    @FXML private TextField companyField;
    @FXML private TextField emailField;
    @FXML private TextField logoField;
    @FXML private Label logoFileLabel;
    @FXML private ImageView logoPreview;
    @FXML private TextField contributionField;
    @FXML private Label errorLabel;

    private Sponsor editing;
    private Sponsor result;
    private String fixedEmail;
    private File selectedLogoFile;

    private Runnable onFormDone;
    private Consumer<Sponsor> onSaved;

    private final CloudinaryUploadService cloud = new CloudinaryUploadService();
    private final SponsorService sponsorService = new SponsorService();

    private static final Pattern EMAIL_RX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public void setOnFormDone(Runnable callback) { this.onFormDone = callback; }
    public void setOnSaved(Consumer<Sponsor> callback) { this.onSaved = callback; }
    public Sponsor getResult() { return result; }

    @FXML
    private void initialize() {
        loadEventTitles();

        if (companyField != null) {
            companyField.textProperty().addListener((obs, old, n) -> {
                if (n == null) return;
                String filtered = n.replaceAll("\\d", "");
                if (filtered.length() > 150) filtered = filtered.substring(0, 150);
                if (!filtered.equals(n)) companyField.setText(filtered);
            });
        }
    }

    private void loadEventTitles() {
        try {
            eventComboBox.setItems(sponsorService.getAllEventTitles());
        } catch (Exception e) {
            error("Erreur chargement événements: " + e.getMessage());
        }
    }

    public void setFixedEmail(String email) {
        this.fixedEmail = (email == null) ? null : email.trim();
        if (this.fixedEmail != null && !this.fixedEmail.isEmpty()) {
            emailField.setText(this.fixedEmail);
            emailField.setDisable(true);
        } else {
            emailField.setDisable(false);
        }
    }

    public void setModeAdd() {
        titleLabel.setText("➕ Nouveau Sponsor");
        editing = null;
        result = null;
        clearErrors();
        loadEventTitles();

        eventComboBox.setValue(null);
        companyField.clear();
        contributionField.clear();

        selectedLogoFile = null;
        logoField.clear();
        logoPreview.setImage(null);

        if (logoFileLabel != null) logoFileLabel.setText("Aucun fichier choisi");

        if (fixedEmail == null) {
            emailField.clear();
            emailField.setDisable(false);
        } else {
            emailField.setText(fixedEmail);
            emailField.setDisable(true);
        }
    }

    public void setModeEdit(Sponsor s) {
        titleLabel.setText("✏ Modifier Sponsor");
        editing = s;
        result = null;
        clearErrors();
        loadEventTitles();

        try {
            String eventTitle = sponsorService.getEventTitleById(s.getEvent_id());
            eventComboBox.setValue(eventTitle);
        } catch (Exception e) {
            // ignore
        }

        companyField.setText(s.getCompany_name());
        contributionField.setText(String.valueOf(s.getContribution_name()));

        logoField.setText(s.getLogo_url() == null ? "" : s.getLogo_url());
        selectedLogoFile = null;

        try {
            if (s.getLogo_url() != null && !s.getLogo_url().isBlank()) {
                logoPreview.setImage(new Image(s.getLogo_url(), true));
            } else {
                logoPreview.setImage(null);
            }
        } catch (Exception e) {
            logoPreview.setImage(null);
        }

        if (logoFileLabel != null) logoFileLabel.setText("Garder logo actuel (ou choisir nouveau)");

        if (fixedEmail != null) {
            emailField.setText(fixedEmail);
            emailField.setDisable(true);
        } else {
            emailField.setText(s.getContact_email());
            emailField.setDisable(false);
        }
    }

    @FXML
    private void onChooseLogo() {
        clearErrors();

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un logo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File f = fc.showOpenDialog(getStage());
        if (f != null) {
            selectedLogoFile = f;
            if (logoFileLabel != null) logoFileLabel.setText(f.getName());
            try { logoPreview.setImage(new Image(f.toURI().toString(), true)); }
            catch (Exception ignored) {}
        }
    }

    @FXML
    private void onSave() {
        clearErrors();

        String selectedEventTitle = eventComboBox.getValue();
        String company = safe(companyField.getText());
        String email = safe(emailField.getText());
        String contribTxt = safe(contributionField.getText()).replace(",", ".");

        if (fixedEmail != null && !fixedEmail.isEmpty()) email = fixedEmail;

        int eventId;
        try {
            if (selectedEventTitle == null || selectedEventTitle.isEmpty()) {
                error("Veuillez sélectionner un événement");
                return;
            }
            eventId = sponsorService.getEventIdByTitle(selectedEventTitle);
        } catch (Exception e) {
            error("Événement invalide: " + e.getMessage());
            return;
        }

        if (company.isEmpty()) { error("Entreprise obligatoire"); return; }
        if (company.length() < 2) { error("Entreprise trop courte"); return; }
        if (company.matches(".*\\d.*")) { error("Entreprise: pas de chiffres"); return; }

        if (email.isEmpty()) { error("Email obligatoire"); return; }
        if (!EMAIL_RX.matcher(email).matches()) { error("Email invalide"); return; }

        double contribution;
        try {
            contribution = Double.parseDouble(contribTxt);
            if (contribution < 0) { error("Contribution doit être >= 0"); return; }
        } catch (NumberFormatException e) {
            error("Contribution invalide (ex: 5000.00)");
            return;
        }

        String logoUrlFinal = safe(logoField.getText());
        try {
            if (selectedLogoFile != null) {
                logoUrlFinal = cloud.uploadLogo(selectedLogoFile);
                logoField.setText(logoUrlFinal);
            }
        } catch (Exception ex) {
            error("Upload logo échoué: " + ex.getMessage());
            return;
        }

        Sponsor out = new Sponsor();
        if (editing != null) out.setId(editing.getId());

        out.setEvent_id(eventId);
        out.setCompany_name(company);
        out.setContact_email(email);
        out.setContribution_name(contribution);
        out.setLogo_url(logoUrlFinal.isEmpty() ? null : logoUrlFinal);

        out.setContract_url(editing == null ? null : editing.getContract_url());
        out.setAccess_code(editing == null ? null : editing.getAccess_code());
        out.setUser_id(editing == null ? null : editing.getUser_id());

        try {
            if (editing == null) sponsorService.addSponsor(out);
            else sponsorService.updateSponsor(out);

            Sponsor saved = sponsorService.getSponsorById(out.getId());
            result = saved != null ? saved : out;

            if (onSaved != null) onSaved.accept(result);
            if (onFormDone != null) onFormDone.run();

            closeWindowIfModal();

        } catch (Exception ex) {
            error("Erreur sauvegarde: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        result = null;
        if (onFormDone != null) onFormDone.run();
        closeWindowIfModal();
    }

    private void closeWindowIfModal() {
        if (onSaved != null) return; // On est en mode page, ne pas fermer la fenêtre
        try {
            Stage stage = getStage();
            if (stage != null && stage.isShowing()) stage.close();
        } catch (Exception ignored) {}
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private void error(String msg) { if (errorLabel != null) errorLabel.setText("❌ " + msg); }
    private void clearErrors() { if (errorLabel != null) errorLabel.setText(""); }

    private Stage getStage() {
        try { return (Stage) eventComboBox.getScene().getWindow(); }
        catch (Exception e) { return null; }
    }
}