package com.example.pidev.controller.sponsor;

import com.example.pidev.model.event.Event;
import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.currency.CurrencyService;
import com.example.pidev.service.external.OpenStreetMapService;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.service.upload.CloudinaryUploadService;
import com.example.pidev.service.whatsapp.WhatsAppService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SponsorFormController {

    @FXML private Label titleLabel;
    @FXML private TextField companyField;
    @FXML private TextField emailField;
    @FXML private TextField logoField;
    @FXML private Label logoFileLabel;
    @FXML private ImageView logoPreview;
    @FXML private TextField contributionField;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label convertedAmountLabel;
    @FXML private TextField phoneField;
    @FXML private TextField industryField;
    @FXML private TextField taxIdField;
    @FXML private Button uploadDocBtn;
    @FXML private Label docFileLabel;

    private Sponsor editing;
    private Sponsor result;
    private Integer selectedEventId;
    private String fixedEmail;

    private File selectedLogoFile;
    private File selectedDocFile;
    private boolean removeLogoRequested;

    private Runnable onFormDone;
    private Consumer<Sponsor> onSaved;

    private final CloudinaryUploadService cloud = new CloudinaryUploadService();
    private final SponsorService sponsorService = new SponsorService();
    private final OpenStreetMapService osmService = new OpenStreetMapService();

    private static final Pattern EMAIL_RX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern TAX_ID_RX = Pattern.compile("^[0-9]{7}[A-Za-z]$");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private static final Map<String, String> KNOWN_BRAND_DOMAINS = Map.ofEntries(
            Map.entry("tunisie telecom", "tunisietelecom.tn"),
            Map.entry("tunisietelecom", "tunisietelecom.tn"),
            Map.entry("ooredoo", "ooredoo.tn"),
            Map.entry("orange tunisie", "orange.tn"),
            Map.entry("orange", "orange.tn"),
            Map.entry("topnet", "topnet.tn")
    );

    public void setOnFormDone(Runnable callback) {
        this.onFormDone = callback;
    }

    public void setOnSaved(Consumer<Sponsor> callback) {
        this.onSaved = callback;
    }

    public Sponsor getResult() {
        return result;
    }

    @FXML
    private void initialize() {
        if (companyField != null) {
            companyField.textProperty().addListener((obs, old, value) -> {
                if (value == null) {
                    return;
                }
                String filtered = value.replaceAll("\\d", "");
                if (filtered.length() > 150) {
                    filtered = filtered.substring(0, 150);
                }
                if (!filtered.equals(value)) {
                    companyField.setText(filtered);
                }
                applyAutoLogoFromEmailIfNeeded(false);
            });
        }

        if (emailField != null) {
            emailField.focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) {
                    applyAutoLogoFromEmailIfNeeded(false);
                }
            });
        }

        if (currencyComboBox != null) {
            currencyComboBox.getItems().addAll(
                    "TND", "USD", "EUR", "GBP", "CHF", "CAD", "JPY", "CNY", "AUD", "NZD",
                    "DKK", "NOK", "SEK", "TRY", "SAR", "AED", "KWD", "BHD", "QAR", "MAD",
                    "EGP", "ZAR", "INR", "PKR", "BDT", "LKR", "MYR", "SGD", "HKD", "KRW",
                    "RUB", "BRL", "MXN", "PLN", "CZK", "HUF", "ILS", "THB", "VND", "PHP"
            );
            currencyComboBox.setValue("TND");
            currencyComboBox.valueProperty().addListener((obs, old, value) -> updateConvertedAmount());
        }

        if (contributionField != null) {
            contributionField.textProperty().addListener((obs, old, value) -> updateConvertedAmount());
        }

        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, old, value) -> {
                if (value == null) {
                    return;
                }
                String filtered = value.replaceAll("[^0-9]", "");
                if (!filtered.equals(value)) {
                    phoneField.setText(filtered);
                }
            });
        }

        if (uploadDocBtn != null) {
            uploadDocBtn.setOnAction(e -> onChooseDocument());
        }

        if (taxIdField != null) {
            taxIdField.focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) {
                    validateTaxId();
                }
            });
        }
    }

    public void setFixedEmail(String email) {
        fixedEmail = safe(email);
        if (fixedEmail.isEmpty()) {
            fixedEmail = null;
        }
        applyEmailLockState();
        if (fixedEmail != null) {
            emailField.setText(fixedEmail);
            applyAutoLogoFromEmailIfNeeded(true);
        }
    }

    public void setModeAdd() {
        titleLabel.setText("Formulaire sponsor");
        editing = null;
        result = null;
        selectedEventId = null;
        clearErrors();

        companyField.clear();
        if (fixedEmail != null) {
            emailField.setText(fixedEmail);
        } else {
            emailField.clear();
        }
        applyEmailLockState();
        contributionField.clear();
        industryField.clear();
        phoneField.clear();
        taxIdField.clear();

        selectedLogoFile = null;
        selectedDocFile = null;
        removeLogoRequested = false;
        logoField.clear();
        logoPreview.setImage(null);

        if (currencyComboBox != null) {
            currencyComboBox.setValue("TND");
        }
        if (convertedAmountLabel != null) {
            convertedAmountLabel.setText("");
        }
        if (logoFileLabel != null) {
            logoFileLabel.setText("Aucun fichier");
        }
        if (docFileLabel != null) {
            docFileLabel.setText("Aucun fichier");
        }

        applyAutoLogoFromEmailIfNeeded(true);
    }

    public void setModeEdit(Sponsor sponsor) {
        titleLabel.setText("Modifier Sponsor");
        editing = sponsor;
        result = null;
        selectedEventId = sponsor == null ? null : sponsor.getEvent_id();
        clearErrors();

        if (sponsor == null) {
            return;
        }

        companyField.setText(safe(sponsor.getCompany_name()));
        if (fixedEmail != null) {
            emailField.setText(fixedEmail);
        } else {
            emailField.setText(safe(sponsor.getContact_email()));
        }
        applyEmailLockState();
        contributionField.setText(String.valueOf(sponsor.getContribution_name()));
        industryField.setText(safe(sponsor.getIndustry()));
        phoneField.setText(safe(sponsor.getPhone()));
        taxIdField.setText(safe(sponsor.getTax_id()));

        String existingLogo = safe(sponsor.getLogo_url());
        logoField.setText(existingLogo);
        selectedLogoFile = null;
        selectedDocFile = null;
        removeLogoRequested = false;

        try {
            if (!existingLogo.isBlank()) {
                Image existingImage = new Image(existingLogo, true);
                existingImage.errorProperty().addListener((obs, wasError, isError) -> {
                    if (Boolean.TRUE.equals(isError) && fixedEmail != null && !fixedEmail.isBlank()) {
                        applyAutoLogoFromEmailIfNeeded(true);
                    }
                });
                logoPreview.setImage(existingImage);
            } else {
                logoPreview.setImage(null);
            }
        } catch (Exception e) {
            logoPreview.setImage(null);
            if (fixedEmail != null && !fixedEmail.isBlank()) {
                applyAutoLogoFromEmailIfNeeded(true);
            }
        }

        if (logoFileLabel != null) {
            logoFileLabel.setText("Garder logo actuel (ou choisir nouveau)");
        }
        if (docFileLabel != null) {
            docFileLabel.setText("Garder document actuel (ou choisir nouveau)");
        }
        if (currencyComboBox != null) {
            currencyComboBox.setValue("TND");
        }
        if (convertedAmountLabel != null) {
            convertedAmountLabel.setText("");
        }

        if (fixedEmail != null && (existingLogo.isBlank() || isAutoGeneratedLogo(existingLogo))) {
            applyAutoLogoFromEmailIfNeeded(true);
        }
    }

    @FXML
    private void onChooseLogo() {
        clearErrors();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un logo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = chooser.showOpenDialog(getStage());
        if (file == null) {
            return;
        }

        selectedLogoFile = file;
        removeLogoRequested = false;
        if (logoFileLabel != null) {
            logoFileLabel.setText(file.getName());
        }

        try {
            Image image = new Image(file.toURI().toString(), true);
            logoPreview.setImage(image);
            logoField.clear();
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void onRemoveLogo() {
        clearErrors();
        selectedLogoFile = null;
        logoField.clear();
        logoPreview.setImage(null);
        // Supprimer puis tenter de recharger le vrai logo entreprise.
        removeLogoRequested = false;
        applyAutoLogoFromEmailIfNeeded(true);
        if (logoFileLabel != null && safe(logoField.getText()).isBlank()) {
            logoFileLabel.setText("Logo supprime. Domaine officiel requis (ex: ooredoo.com).");
        }
    }

    @FXML
    private void onChooseDocument() {
        clearErrors();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un justificatif (image ou PDF)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        File file = chooser.showOpenDialog(getStage());
        if (file != null) {
            selectedDocFile = file;
            if (docFileLabel != null) {
                docFileLabel.setText(file.getName());
            }
        }
    }

    @FXML
    private void onSave() {
        clearErrors();

        int eventId;
        if (editing != null) {
            eventId = editing.getEvent_id();
        } else if (selectedEventId != null) {
            eventId = selectedEventId;
        } else if (fixedEmail == null || fixedEmail.isBlank()) {
            // Admin form: event link is optional.
            eventId = 0;
        } else {
            error("Choisissez un evenement via le bouton Sponsoriser.");
            return;
        }

        String company = safe(companyField.getText());
        String email = fixedEmail != null ? fixedEmail : safe(emailField.getText());
        if (fixedEmail != null) {
            emailField.setText(fixedEmail);
        }
        String contributionText = safe(contributionField.getText()).replace(",", ".");
        String industry = safe(industryField.getText());
        String phone = safe(phoneField.getText());
        String taxId = safe(taxIdField.getText());

        if (company.isEmpty()) {
            error("Entreprise obligatoire");
            return;
        }
        if (company.length() < 2) {
            error("Entreprise trop courte");
            return;
        }
        if (company.matches(".*\\d.*")) {
            error("Entreprise: pas de chiffres");
            return;
        }

        if (email.isEmpty()) {
            error("Email obligatoire");
            return;
        }
        if (!EMAIL_RX.matcher(email).matches()) {
            error("Email invalide");
            return;
        }

        if (!taxId.isEmpty() && !TAX_ID_RX.matcher(taxId).matches()) {
            error("Format No Fiscal invalide (7 chiffres + 1 lettre)");
            return;
        }

        String currency = currencyComboBox == null ? "TND" : currencyComboBox.getValue();
        double originalAmount;
        try {
            originalAmount = Double.parseDouble(contributionText);
            if (originalAmount <= 0) {
                error("Contribution doit etre > 0");
                return;
            }
        } catch (NumberFormatException e) {
            error("Contribution invalide (ex: 5000.00)");
            return;
        }

        double contributionInTnd;
        if (currency == null || "TND".equals(currency)) {
            contributionInTnd = originalAmount;
        } else {
            contributionInTnd = CurrencyService.convert(originalAmount, currency, "TND");
            if (contributionInTnd < 0) {
                error("Erreur de conversion de devise.");
                return;
            }
        }

        String logoUrlFinal = safe(logoField.getText());
        if (removeLogoRequested) {
            logoUrlFinal = "";
        }
        try {
            if (!removeLogoRequested && selectedLogoFile != null) {
                logoUrlFinal = cloud.uploadLogo(selectedLogoFile);
                logoField.setText(logoUrlFinal);
            }
        } catch (Exception ex) {
            error("Upload logo echoue: " + ex.getMessage());
            return;
        }
        if (!removeLogoRequested && selectedLogoFile == null && logoUrlFinal.isEmpty()) {
            applyAutoLogoFromEmailIfNeeded(true);
            logoUrlFinal = safe(logoField.getText());
        }

        String docUrlFinal = editing == null ? null : editing.getDocument_url();
        try {
            if (selectedDocFile != null) {
                docUrlFinal = cloud.uploadDocument(selectedDocFile);
            }
        } catch (Exception ex) {
            error("Upload document echoue: " + ex.getMessage());
            return;
        }

        Sponsor out = new Sponsor();
        if (editing != null) {
            out.setId(editing.getId());
        }

        out.setEvent_id(eventId);
        out.setCompany_name(company);
        out.setContact_email(email);
        out.setContribution_name(contributionInTnd);
        out.setLogo_url(logoUrlFinal.isEmpty() ? null : logoUrlFinal);
        out.setIndustry(industry);
        out.setPhone(phone);
        out.setTax_id(taxId);
        out.setDocument_url(docUrlFinal);

        out.setContract_url(editing == null ? null : editing.getContract_url());
        out.setAccess_code(editing == null ? null : editing.getAccess_code());
        out.setUser_id(editing == null ? null : editing.getUser_id());

        if (!company.isEmpty()) {
            new Thread(() -> {
                try {
                    String displayName = osmService.searchCompany(company);
                    Platform.runLater(() -> {
                        if (displayName != null) {
                            showInfo("Verification OpenStreetMap", "Entreprise trouvee:\n" + displayName);
                        } else {
                            showWarning("Verification OpenStreetMap", "Aucune correspondance trouvee.");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> error("Erreur recherche entreprise: " + e.getMessage()));
                }
            }).start();
        }

        try {
            if (editing == null) {
                sponsorService.addSponsor(out);
            } else {
                sponsorService.updateSponsor(out);
            }

            Sponsor saved = sponsorService.getSponsorById(out.getId());
            result = saved != null ? saved : out;

            if (!phone.isEmpty()) {
                new Thread(() -> {
                    boolean sent = WhatsAppService.sendConfirmation(phone, company, contributionInTnd);
                    if (sent) {
                        System.out.println("Message WhatsApp envoye a " + phone);
                    } else {
                        System.err.println("Echec de l'envoi WhatsApp");
                        String reason = WhatsAppService.getLastError();
                        Platform.runLater(() -> showWarning(
                                "WhatsApp indisponible",
                                "Le message de confirmation n'a pas ete envoye.\n" +
                                        (reason == null || reason.isBlank()
                                                ? "Verifiez la configuration Twilio (SID, token, sandbox)."
                                                : reason)
                        ));
                    }
                }).start();
            }

            showSuccess("Succes", "Sponsor enregistre avec succes.");

            if (onSaved != null) {
                onSaved.accept(result);
            }
            if (onFormDone != null) {
                onFormDone.run();
            }

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

    public void preSelectEvent(Event event) {
        if (event != null) {
            selectedEventId = event.getId();
        }
    }

    private void validateTaxId() {
        String taxId = safe(taxIdField.getText());
        if (!taxId.isEmpty() && !TAX_ID_RX.matcher(taxId).matches()) {
            taxIdField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
            error("Format No Fiscal invalide (ex: 1234567A)");
        } else {
            taxIdField.setStyle("-fx-border-color: #cbd5e1; -fx-border-width: 1.5;");
            clearErrors();
        }
    }

    private void updateConvertedAmount() {
        if (currencyComboBox == null || contributionField == null || convertedAmountLabel == null) {
            return;
        }

        String currency = currencyComboBox.getValue();
        String amountText = contributionField.getText().trim().replace(",", ".");
        if (amountText.isEmpty()) {
            convertedAmountLabel.setText("");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (currency == null || "TND".equals(currency)) {
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

    private void closeWindowIfModal() {
        if (onSaved != null) {
            return;
        }
        try {
            Stage stage = getStage();
            if (stage != null && stage.isShowing()) {
                stage.close();
            }
        } catch (Exception ignored) {
        }
    }

    private Stage getStage() {
        try {
            return (Stage) companyField.getScene().getWindow();
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void applyEmailLockState() {
        if (emailField == null) {
            return;
        }
        boolean locked = fixedEmail != null && !fixedEmail.isBlank();
        emailField.setDisable(false);
        emailField.setEditable(!locked);
        emailField.setFocusTraversable(!locked);
    }

    private void applyAutoLogoFromEmailIfNeeded(boolean force) {
        if (removeLogoRequested || selectedLogoFile != null) {
            return;
        }
        if (logoField == null || logoPreview == null) {
            return;
        }
        String currentLogo = safe(logoField.getText());
        if (!force && !currentLogo.isEmpty() && !isAutoGeneratedLogo(currentLogo)) {
            return;
        }
        String email = fixedEmail != null ? fixedEmail : safe(emailField == null ? null : emailField.getText());
        String company = safe(companyField == null ? null : companyField.getText());
        String domain = resolveBrandDomain(company, email);

        String brandLogoUrl = findBestBrandLogoUrl(company, email);
        if (brandLogoUrl.isBlank()) {
            logoField.clear();
            logoPreview.setImage(null);
            if (logoFileLabel != null) {
                if (domain.isBlank()) {
                    logoFileLabel.setText("Domaine introuvable. Saisir entreprise comme domaine (ex: ooredoo.com).");
                } else {
                    logoFileLabel.setText("Logo reel introuvable pour: " + domain);
                }
            }
            return;
        }

        logoField.setText(brandLogoUrl);
        try {
            Image brandImage = new Image(brandLogoUrl, true);
            brandImage.errorProperty().addListener((obs, wasError, isError) -> {
                if (Boolean.TRUE.equals(isError)) {
                    logoField.clear();
                    logoPreview.setImage(null);
                    if (logoFileLabel != null) {
                        logoFileLabel.setText("Logo reel indisponible. Verifier le domaine officiel.");
                    }
                }
            });
            logoPreview.setImage(brandImage);
            if (logoFileLabel != null) {
                logoFileLabel.setText("Logo marque par domaine: " + domain);
            }
        } catch (Exception ignored) {
            logoField.clear();
            logoPreview.setImage(null);
            if (logoFileLabel != null) {
                logoFileLabel.setText("Logo reel indisponible. Verifier le domaine officiel.");
            }
        }
    }

    private String findBestBrandLogoUrl(String company, String email) {
        List<String> domains = buildDomainCandidates(company, email);
        for (String d : domains) {
            String clearbit = buildBrandLogoUrl(d);
            if (isReachableImage(clearbit)) {
                return clearbit;
            }
            String favicon = buildGoogleFaviconUrl(d);
            if (isReachableImage(favicon)) {
                return favicon;
            }
        }
        return "";
    }

    private List<String> buildDomainCandidates(String company, String email) {
        Set<String> out = new LinkedHashSet<>();

        String domainFromMapping = resolveKnownDomain(company);
        if (!domainFromMapping.isBlank()) {
            out.add(domainFromMapping);
        }

        String domainFromCompany = extractDomainFromRaw(company);
        if (!domainFromCompany.isBlank()) {
            out.add(domainFromCompany);
        }

        String normalizedCompany = normalizeCompany(company).replace(" ", "");
        if (!normalizedCompany.isBlank()) {
            out.add(normalizedCompany + ".com");
            out.add(normalizedCompany + ".tn");
        }

        String companySlug = normalizeCompany(company).replace(" ", "-");
        if (!companySlug.isBlank()) {
            out.add(companySlug + ".com");
            out.add(companySlug + ".tn");
        }

        // Email domain is fallback only: company domain has priority.
        String fromEmail = extractDomain(email);
        if (!fromEmail.isBlank()) {
            out.add(fromEmail);
        }

        return new ArrayList<>(out);
    }

    private boolean isReachableImage(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> resp = HTTP.send(req, HttpResponse.BodyHandlers.discarding());
            int status = resp.statusCode();
            return status >= 200 && status < 400;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveBrandDomain(String company, String email) {
        List<String> candidates = buildDomainCandidates(company, email);
        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }
        return "";
    }

    private String resolveKnownDomain(String company) {
        String normalized = normalizeCompany(company);
        if (normalized.isBlank()) {
            return "";
        }
        for (Map.Entry<String, String> entry : KNOWN_BRAND_DOMAINS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    private String normalizeCompany(String company) {
        String value = safe(company).toLowerCase();
        if (value.isBlank()) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String extractDomainFromRaw(String rawValue) {
        String value = safe(rawValue).toLowerCase();
        if (value.isBlank()) {
            return "";
        }
        value = value.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "");
        int slash = value.indexOf('/');
        if (slash > -1) {
            value = value.substring(0, slash);
        }
        value = value.trim();
        if (value.matches("^[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}$")) {
            return value;
        }
        return "";
    }

    private String buildBrandLogoUrl(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        return "https://logo.clearbit.com/" + domain + "?size=256";
    }

    private String buildGoogleFaviconUrl(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        String encoded = URLEncoder.encode(domain, StandardCharsets.UTF_8);
        return "https://www.google.com/s2/favicons?domain=" + encoded + "&sz=128";
    }

    private String buildLogoUrlFromEmail(String email) {
        String domain = extractDomain(email);
        if (domain.isEmpty()) {
            return buildAvatarUrlFromEmail(email);
        }
        return "https://logo.clearbit.com/" + domain + "?size=256";
    }

    private String buildAvatarUrlFromEmail(String email) {
        String value = safe(email);
        if (value.isEmpty()) {
            return "";
        }
        String local = extractLocalPart(value);
        if (isGenericLocalPart(local)) {
            String domainRoot = extractDomainRoot(value);
            if (!domainRoot.isBlank()) {
                local = domainRoot;
            }
        }
        String encoded = URLEncoder.encode(local, StandardCharsets.UTF_8);
        return "https://ui-avatars.com/api/?name=" + encoded +
                "&background=0D47A1&color=ffffff&size=256&bold=true&format=png";
    }

    private String extractDomain(String email) {
        String value = safe(email).toLowerCase();
        int at = value.lastIndexOf('@');
        if (at <= 0 || at >= value.length() - 1) {
            return "";
        }
        String domain = value.substring(at + 1).trim();
        if (domain.isEmpty() || !domain.contains(".")) {
            return "";
        }
        return domain;
    }

    private void setGeneratedLocalLogo(String email) {
        String initials = getInitialsFromEmail(email);
        if (initials.isEmpty()) {
            initials = "SP";
        }

        int hash = Math.abs(safe(email).toLowerCase().hashCode());
        double hue = hash % 360;
        Color background = Color.hsb(hue, 0.68, 0.74);

        Canvas canvas = new Canvas(256, 256);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(background);
        gc.fillRoundRect(0, 0, 256, 256, 48, 48);
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 96));
        gc.fillText(initials, 128, 136);

        WritableImage image = new WritableImage(256, 256);
        canvas.snapshot(new SnapshotParameters(), image);
        logoPreview.setImage(image);

        if (logoField != null) {
            String localLogoUri = saveGeneratedLogoToLocal(image, email);
            if (localLogoUri != null && !localLogoUri.isBlank()) {
                logoField.setText(localLogoUri);
            } else {
                logoField.setText(buildAvatarUrlFromEmail(email));
            }
        }
        if (logoFileLabel != null) {
            logoFileLabel.setText("Logo local genere depuis l'email");
        }
    }

    private String saveGeneratedLogoToLocal(WritableImage image, String email) {
        try {
            String safeMail = safe(email).toLowerCase().replaceAll("[^a-z0-9@._-]", "");
            if (safeMail.isBlank()) {
                safeMail = "sponsor";
            }
            safeMail = safeMail.replace("@", "_at_");

            Path logoDir = Paths.get(System.getProperty("user.home"), ".eventflow", "generated-logos");
            Files.createDirectories(logoDir);

            Path logoPath = logoDir.resolve("logo_" + safeMail + ".png");
            BufferedImage buffered = toBufferedImage(image);
            ImageIO.write(buffered, "png", logoPath.toFile());
            return logoPath.toUri().toString();
        } catch (IOException ignored) {
            return "";
        }
    }

    private BufferedImage toBufferedImage(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private String getInitialsFromEmail(String sourceValue) {
        String value = safe(sourceValue);
        if (value.isEmpty()) {
            return "";
        }
        String source = value;
        if (value.contains("@")) {
            source = extractLocalPart(value);
        }
        if (isGenericLocalPart(source)) {
            String domainRoot = extractDomainRoot(value);
            if (!domainRoot.isBlank()) {
                source = domainRoot;
            }
        }

        String cleaned = source.replaceAll("[^A-Za-z0-9]", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        String[] parts = cleaned.split("\\s+");
        if (parts.length == 1) {
            String p = parts[0].toUpperCase();
            return p.length() >= 2 ? p.substring(0, 2) : p.substring(0, 1);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String extractLocalPart(String email) {
        String value = safe(email);
        int at = value.indexOf('@');
        return at > 0 ? value.substring(0, at) : value;
    }

    private String extractDomainRoot(String email) {
        String domain = extractDomain(email);
        if (domain.isBlank()) {
            return "";
        }
        String[] parts = domain.split("\\.");
        if (parts.length == 0) {
            return "";
        }
        return parts[0];
    }

    private boolean isGenericLocalPart(String localPart) {
        String v = safe(localPart).toLowerCase();
        return v.equals("sponsor")
                || v.equals("admin")
                || v.equals("user")
                || v.equals("contact")
                || v.equals("info")
                || v.equals("support")
                || v.equals("mail")
                || v.equals("noreply")
                || v.equals("no-reply");
    }

    private boolean isAutoGeneratedLogo(String logoUrl) {
        String value = safe(logoUrl).toLowerCase();
        return value.contains("logo.clearbit.com")
                || value.contains("google.com/s2/favicons")
                || value.contains("ui-avatars.com")
                || value.contains("/.eventflow/generated-logos/")
                || value.contains("\\.eventflow\\generated-logos\\");
    }

    private void error(String message) {
        if (errorLabel != null) {
            errorLabel.setText("Erreur: " + message);
        }
    }

    private void clearErrors() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
