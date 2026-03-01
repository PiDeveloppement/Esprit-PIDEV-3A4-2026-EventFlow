package com.example.pidev.controller.sponsor;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.service.upload.CloudinaryUploadService;
import com.example.pidev.service.imagga.ImaggaService;
import com.example.pidev.service.currency.CurrencyService;
import com.example.pidev.service.whatsapp.WhatsAppService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    @FXML private Button analyzeLogoBtn;
    @FXML private TextArea visionResultArea;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label convertedAmountLabel;
    @FXML private TextField phoneField;
    @FXML private TextField industryField;
    @FXML private TextField taxIdField;          // NOUVEAU
    @FXML private Button uploadDocBtn;            // NOUVEAU
    @FXML private Label docFileLabel;             // NOUVEAU
    @FXML private Button importDocBtn;  // (optionnel)

    private Sponsor editing;
    private Sponsor result;
    private String fixedEmail;
    private File selectedLogoFile;
    private File selectedDocFile;                  // NOUVEAU

    private Runnable onFormDone;
    private Consumer<Sponsor> onSaved;

    private final CloudinaryUploadService cloud = new CloudinaryUploadService();
    private final SponsorService sponsorService = new SponsorService();

    private static final Pattern EMAIL_RX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    // Regex pour numéro fiscal tunisien : 7 chiffres + une lettre (ex: 1234567A)
    private static final Pattern TAX_ID_RX = Pattern.compile("^[0-9]{7}[A-Za-z]$");

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

        if (analyzeLogoBtn != null) {
            analyzeLogoBtn.setOnAction(e -> onAnalyzeLogo());
        }

        if (currencyComboBox != null) {
            currencyComboBox.getItems().addAll(
                    "TND", "USD", "EUR", "GBP", "CHF", "CAD", "JPY", "CNY", "AUD", "NZD",
                    "DKK", "NOK", "SEK", "TRY", "SAR", "AED", "KWD", "BHD", "QAR", "MAD",
                    "EGP", "ZAR", "INR", "PKR", "BDT", "LKR", "MYR", "SGD", "HKD", "KRW",
                    "RUB", "BRL", "MXN", "PLN", "CZK", "HUF", "ILS", "THB", "VND", "PHP"
            );
            currencyComboBox.setValue("TND");
            currencyComboBox.valueProperty().addListener((obs, old, n) -> updateConvertedAmount());
        }

        if (contributionField != null) {
            contributionField.textProperty().addListener((obs, old, n) -> updateConvertedAmount());
        }

        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, old, n) -> {
                if (n == null) return;
                String filtered = n.replaceAll("[^0-9]", "");
                if (!filtered.equals(n)) phoneField.setText(filtered);
            });
        }

        // Gestionnaire pour le bouton d'upload de document
        if (uploadDocBtn != null) {
            uploadDocBtn.setOnAction(e -> onChooseDocument());
        }

        // Validation en temps réel du numéro fiscal (optionnel)
        if (taxIdField != null) {
            taxIdField.focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) {
                    validateTaxId();
                }
            });
        }
    }

    private void validateTaxId() {
        String taxId = safe(taxIdField.getText());
        if (!taxId.isEmpty() && !TAX_ID_RX.matcher(taxId).matches()) {
            // Afficher un indicateur d'erreur (bordure rouge)
            taxIdField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
            error("Format de numéro fiscal invalide (ex: 1234567A)");
        } else {
            taxIdField.setStyle("-fx-border-color: #cbd5e1; -fx-border-width: 1.5;");
            clearErrors();
        }
    }

    private void updateConvertedAmount() {
        if (currencyComboBox == null || contributionField == null || convertedAmountLabel == null) return;
        String currency = currencyComboBox.getValue();
        String amountText = contributionField.getText().trim().replace(",", ".");
        if (amountText.isEmpty()) {
            convertedAmountLabel.setText("");
            return;
        }
        try {
            double amount = Double.parseDouble(amountText);
            if (currency == null || currency.equals("TND")) {
                convertedAmountLabel.setText(String.format("= %,.2f TND", amount));
            } else {
                double converted = CurrencyService.convert(amount, currency, "TND");
                if (converted >= 0) {
                    convertedAmountLabel.setText(String.format("≈ %,.2f TND", converted));
                } else {
                    convertedAmountLabel.setText("Erreur conversion");
                }
            }
        } catch (NumberFormatException e) {
            convertedAmountLabel.setText("Montant invalide");
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
        industryField.clear();
        phoneField.clear();
        taxIdField.clear();          // nouveau

        selectedLogoFile = null;
        selectedDocFile = null;      // nouveau
        logoField.clear();
        logoPreview.setImage(null);
        if (visionResultArea != null) visionResultArea.clear();
        if (currencyComboBox != null) currencyComboBox.setValue("TND");
        if (convertedAmountLabel != null) convertedAmountLabel.setText("");
        if (logoFileLabel != null) logoFileLabel.setText("Aucun fichier choisi");
        if (docFileLabel != null) docFileLabel.setText("Aucun fichier choisi");

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
        industryField.setText(s.getIndustry());
        phoneField.setText(s.getPhone());
        taxIdField.setText(s.getTax_id());          // nouveau

        logoField.setText(s.getLogo_url() == null ? "" : s.getLogo_url());
        selectedLogoFile = null;
        selectedDocFile = null;                      // nouveau

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
        if (docFileLabel != null) docFileLabel.setText("Garder document actuel (ou choisir nouveau)");
        if (visionResultArea != null) visionResultArea.clear();
        if (currencyComboBox != null) currencyComboBox.setValue("TND");
        if (convertedAmountLabel != null) convertedAmountLabel.setText("");

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
            try {
                Image img = new Image(f.toURI().toString(), true);
                logoPreview.setImage(img);
                logoField.clear();
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void onChooseDocument() {                // NOUVELLE MÉTHODE
        clearErrors();

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un justificatif (image ou PDF)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        File f = fc.showOpenDialog(getStage());
        if (f != null) {
            selectedDocFile = f;
            if (docFileLabel != null) docFileLabel.setText(f.getName());
        }
    }

    @FXML
    private void onAnalyzeLogo() {
        if (selectedLogoFile == null) {
            showError("Veuillez d'abord sélectionner un fichier logo.");
            return;
        }

        analyzeLogoBtn.setDisable(true);
        if (visionResultArea != null) visionResultArea.setText("Analyse en cours...");

        new Thread(() -> {
            try {
                JsonArray tags = ImaggaService.analyzeImageFromFile(selectedLogoFile);

                Platform.runLater(() -> {
                    analyzeLogoBtn.setDisable(false);
                    if (tags == null) {
                        visionResultArea.setText("Erreur : l'API n'a pas retourné de résultats. Vérifiez la console pour les détails.");
                        return;
                    }
                    if (tags.isEmpty()) {
                        visionResultArea.setText("Aucun tag détecté. L'image n'est peut-être pas valide.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder("Résultats de l'analyse (Imagga) :\n");
                    double bestConfidence = 0.0;
                    String bestTag = "";
                    int displayed = 0;
                    for (int i = 0; i < tags.size() && displayed < 20; i++) {
                        JsonObject tag = tags.get(i).getAsJsonObject();
                        double confidence = tag.get("confidence").getAsDouble();
                        JsonObject tagInfo = tag.getAsJsonObject("tag");
                        String tagName = tagInfo.get("en").getAsString();
                        sb.append(String.format("- %s : %.2f%%\n", tagName, confidence));
                        displayed++;

                        String lower = tagName.toLowerCase();
                        if (lower.contains("logo") || lower.contains("brand") || lower.contains("emblem") ||
                                lower.contains("icon") || lower.contains("symbol") || lower.contains("trademark") ||
                                lower.contains("logotype") || lower.contains("insignia") || lower.contains("company") ||
                                lower.contains("corporate")) {
                            if (confidence > bestConfidence) {
                                bestConfidence = confidence;
                                bestTag = tagName;
                            }
                        }
                    }

                    if (bestConfidence > 0) {
                        sb.append(String.format("\n🔍 Meilleur indicateur : '%s' avec %.2f%%", bestTag, bestConfidence));
                        sb.append(String.format("\n📊 Confiance que c'est un logo : %.2f%%", bestConfidence));
                    } else {
                        sb.append("\n📊 Confiance que c'est un logo : 0.00% (aucun tag spécifique détecté)");
                    }

                    if (bestConfidence >= 40.0) {
                        sb.append("\n✅ Forte probabilité que ce soit un logo !");
                    } else if (bestConfidence >= 25.0) {
                        sb.append("\n⚠️ Probabilité moyenne que ce soit un logo");
                    } else if (bestConfidence >= 15.0) {
                        sb.append("\n❓ Faible probabilité que ce soit un logo");
                    } else if (bestConfidence > 0) {
                        sb.append("\n❓ Très faible probabilité que ce soit un logo");
                    } else {
                        sb.append("\n❌ Ne ressemble pas à un logo");
                    }

                    visionResultArea.setText(sb.toString());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    analyzeLogoBtn.setDisable(false);
                    visionResultArea.setText("Erreur lors de l'analyse : " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void onSave() {
        clearErrors();

        String selectedEventTitle = eventComboBox.getValue();
        String company = safe(companyField.getText());
        String email = safe(emailField.getText());
        String contribTxt = safe(contributionField.getText()).replace(",", ".");
        String industry = safe(industryField.getText());
        String phone = safe(phoneField.getText());
        String taxId = safe(taxIdField.getText());   // nouveau

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

        // Validation du numéro fiscal (optionnel mais si présent, doit être valide)
        if (!taxId.isEmpty() && !TAX_ID_RX.matcher(taxId).matches()) {
            error("Format de numéro fiscal invalide (7 chiffres + lettre)");
            return;
        }

        String currency = (currencyComboBox == null) ? "TND" : currencyComboBox.getValue();
        double originalAmount;
        try {
            originalAmount = Double.parseDouble(contribTxt);
            if (originalAmount <= 0) { error("Contribution doit être > 0"); return; }
        } catch (NumberFormatException e) {
            error("Contribution invalide (ex: 5000.00)");
            return;
        }

        double contributionInTND;
        if (currency.equals("TND")) {
            contributionInTND = originalAmount;
        } else {
            contributionInTND = CurrencyService.convert(originalAmount, currency, "TND");
            if (contributionInTND < 0) {
                error("Erreur de conversion de devise. Vérifiez votre connexion ou les codes.");
                return;
            }
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

        String docUrlFinal = null;
        try {
            if (selectedDocFile != null) {
                docUrlFinal = cloud.uploadDocument(selectedDocFile);
            }
        } catch (Exception ex) {
            error("Upload document échoué: " + ex.getMessage());
            return;
        }

        Sponsor out = new Sponsor();
        if (editing != null) out.setId(editing.getId());

        out.setEvent_id(eventId);
        out.setCompany_name(company);
        out.setContact_email(email);
        out.setContribution_name(contributionInTND);
        out.setLogo_url(logoUrlFinal.isEmpty() ? null : logoUrlFinal);
        out.setIndustry(industry);
        out.setPhone(phone);
        out.setTax_id(taxId);                // nouveau
        out.setDocument_url(docUrlFinal);     // nouveau

        out.setContract_url(editing == null ? null : editing.getContract_url());
        out.setAccess_code(editing == null ? null : editing.getAccess_code());
        out.setUser_id(editing == null ? null : editing.getUser_id());

        try {
            if (editing == null) sponsorService.addSponsor(out);
            else sponsorService.updateSponsor(out);

            Sponsor saved = sponsorService.getSponsorById(out.getId());
            result = saved != null ? saved : out;

            if (!phone.isEmpty()) {
                new Thread(() -> {
                    boolean sent = WhatsAppService.sendConfirmation(phone, company, contributionInTND);
                    if (sent) {
                        System.out.println("Message WhatsApp envoyé à " + phone);
                    } else {
                        System.err.println("Échec de l'envoi WhatsApp");
                    }
                }).start();
            }

            if (onSaved != null) onSaved.accept(result);
            if (onFormDone != null) onFormDone.run();

            closeWindowIfModal();

        } catch (Exception ex) {
            error("Erreur sauvegarde: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (onFormDone != null) {
            onFormDone.run();
        } else {
            closeWindowIfModal();
        }
    }

    private void closeWindowIfModal() {
        if (onSaved != null) return;
        try {
            Stage stage = getStage();
            if (stage != null && stage.isShowing()) stage.close();
        } catch (Exception ignored) {}
    }

    public void preSelectEvent(Event event) {
        if (event != null) {
            String title = event.getTitle();
            if (eventComboBox.getItems().contains(title)) {
                eventComboBox.setValue(title);
            } else {
                try {
                    ObservableList<String> titles = sponsorService.getAllEventTitles();
                    if (titles.contains(title)) {
                        eventComboBox.setItems(titles);
                        eventComboBox.setValue(title);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private void error(String msg) { if (errorLabel != null) errorLabel.setText("❌ " + msg); }
    private void clearErrors() { if (errorLabel != null) errorLabel.setText(""); }
    private void showError(String msg) { if (errorLabel != null) errorLabel.setText("❌ " + msg); }

    private Stage getStage() {
        try { return (Stage) eventComboBox.getScene().getWindow(); }
        catch (Exception e) { return null; }
    }
}