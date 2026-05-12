package com.example.pidev.controller.sponsor;

import com.example.pidev.model.event.Event;
import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.currency.CurrencyService;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.service.upload.CloudinaryUploadService;
import com.example.pidev.service.whatsapp.WhatsAppService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SponsorFormController {

    private static final Properties LOCAL_CONFIG = loadLocalConfig();

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label     titleLabel;
    @FXML private TextField companyField;
    @FXML private TextField emailField;
    @FXML private TextField logoField;
    @FXML private Label     logoFileLabel;
    @FXML private ImageView logoPreview;
    @FXML private TextField contributionField;
    @FXML private Label     errorLabel;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label     convertedAmountLabel;
    @FXML private TextField phoneField;
    @FXML private TextField industryField;
    @FXML private TextField taxIdField;
    @FXML private Button    uploadDocBtn;
    @FXML private Label     docFileLabel;

    // ── État ──────────────────────────────────────────────────────────────────
    private Sponsor  editing;
    private Sponsor  result;
    private Integer  selectedEventId;
    private String   fixedEmail;

    private File    selectedLogoFile;
    private File    selectedDocFile;
    private boolean removeLogoRequested;

    private Runnable          onFormDone;
    private Consumer<Sponsor> onSaved;

    /**
     * Timer pour le debounce : on attend 800ms après la dernière frappe
     * avant de lancer la recherche du logo.
     */
    private Timer debounceTimer;

    // ── Services ──────────────────────────────────────────────────────────────
    private final CloudinaryUploadService cloud          = new CloudinaryUploadService();
    private final SponsorService          sponsorService = new SponsorService();

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final Pattern EMAIL_RX  = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern TAX_ID_RX = Pattern.compile("^[0-9]{7}[A-Za-z]$");

    /** Délai debounce en ms : on attend que l'utilisateur finisse de taper */
    private static final long DEBOUNCE_MS = 800;

    /** Longueur minimale du nom d'entreprise avant de chercher un logo */
    private static final int MIN_COMPANY_LENGTH = 3;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Correspondances entreprise → domaine officiel.
     * La clé DOIT être le nom exact normalisé (minuscules, sans accents).
     * On utilise une correspondance EXACTE pour éviter les faux positifs.
     */
    private static final Map<String, String> KNOWN_BRAND_DOMAINS = Map.ofEntries(
            Map.entry("tunisie telecom",  "tunisietelecom.tn"),
            Map.entry("tunisietelecom",   "tunisietelecom.tn"),
            Map.entry("ooredoo",          "ooredoo.tn"),
            Map.entry("ooredoo tunisie",  "ooredoo.tn"),
            Map.entry("orange",           "orange.tn"),
            Map.entry("orange tunisie",   "orange.tn"),
            Map.entry("topnet",           "topnet.tn"),
            Map.entry("attijari",         "attijaribank.tn"),
            Map.entry("biat",             "biat.com.tn"),
            Map.entry("stb",              "stb.com.tn"),
            Map.entry("amen bank",        "amenbank.com.tn"),
            Map.entry("banque zitouna",   "banquezitouna.tn"),
            Map.entry("poulina",          "poulinagroup.com"),
            Map.entry("sfbt",             "sfbt.com.tn"),
            Map.entry("actia",            "actia.fr"),
            Map.entry("vermeg",           "vermeg.com"),
            Map.entry("telnet",           "telnet.tn")
    );

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    private void initialize() {
        if (companyField != null) {
            companyField.textProperty().addListener((obs, old, value) -> {
                if (value == null) return;
                // Filtre les chiffres
                String filtered = value.replaceAll("\\d", "");
                if (filtered.length() > 150) filtered = filtered.substring(0, 150);
                if (!filtered.equals(value)) {
                    companyField.setText(filtered);
                    return; // le listener sera rappelé avec la valeur filtrée
                }
                // ✅ Debounce : annule la recherche précédente, relance après 800ms
                scheduleLogoSearch();
            });
        }

        // Recherche logo quand l'email perd le focus (une seule fois)
        if (emailField != null) {
            emailField.focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) scheduleLogoSearch();
            });
        }

        // Devises
        if (currencyComboBox != null) {
            currencyComboBox.getItems().addAll(
                    "TND","USD","EUR","GBP","CHF","CAD","JPY","CNY","AUD","NZD",
                    "DKK","NOK","SEK","TRY","SAR","AED","KWD","BHD","QAR","MAD",
                    "EGP","ZAR","INR","PKR","BDT","LKR","MYR","SGD","HKD","KRW",
                    "RUB","BRL","MXN","PLN","CZK","HUF","ILS","THB","VND","PHP"
            );
            currencyComboBox.setValue("TND");
            currencyComboBox.valueProperty().addListener((obs, old, v) -> updateConvertedAmount());
        }

        if (contributionField != null)
            contributionField.textProperty().addListener((obs, old, v) -> updateConvertedAmount());

        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, old, value) -> {
                if (value == null) return;
                String filtered = value.replaceAll("[^0-9]", "");
                if (!filtered.equals(value)) phoneField.setText(filtered);
            });
        }

        if (uploadDocBtn != null) uploadDocBtn.setOnAction(e -> onChooseDocument());

        if (taxIdField != null)
            taxIdField.focusedProperty().addListener((obs, old, focused) -> {
                if (!focused) validateTaxId();
            });
    }

    // =========================================================================
    //  API PUBLIQUE
    // =========================================================================

    public void setOnFormDone(Runnable callback)          { this.onFormDone = callback; }
    public void setOnSaved(Consumer<Sponsor> callback)    { this.onSaved    = callback; }
    public Sponsor getResult()                            { return result; }

    public void setFixedEmail(String email) {
        fixedEmail = safe(email);
        if (fixedEmail.isEmpty()) fixedEmail = null;
        applyEmailLockState();
        if (fixedEmail != null) {
            emailField.setText(fixedEmail);
            // Ne pas chercher le logo ici — attendre que le nom d'entreprise soit saisi
        }
    }

    public void setModeAdd() {
        titleLabel.setText("Formulaire sponsor");
        editing = null; result = null; selectedEventId = null;
        clearErrors();

        companyField.clear();
        emailField.setText(fixedEmail != null ? fixedEmail : "");
        applyEmailLockState();
        contributionField.clear();
        industryField.clear();
        phoneField.clear();
        taxIdField.clear();

        selectedLogoFile = null; selectedDocFile = null; removeLogoRequested = false;
        logoField.clear();
        logoPreview.setImage(null);

        if (currencyComboBox     != null) currencyComboBox.setValue("TND");
        if (convertedAmountLabel != null) convertedAmountLabel.setText("");
        if (logoFileLabel        != null) logoFileLabel.setText("Aucun fichier");
        if (docFileLabel         != null) docFileLabel.setText("Aucun fichier");
    }

    public void setModeEdit(Sponsor sponsor) {
        titleLabel.setText("Modifier Sponsor");
        editing = sponsor; result = null;
        selectedEventId = sponsor == null ? null : sponsor.getEvent_id();
        clearErrors();
        if (sponsor == null) return;

        companyField.setText(safe(sponsor.getCompany_name()));
        emailField.setText(fixedEmail != null ? fixedEmail : safe(sponsor.getContact_email()));
        applyEmailLockState();
        contributionField.setText(String.valueOf(sponsor.getContribution_name()));
        industryField.setText(safe(sponsor.getIndustry()));
        phoneField.setText(safe(sponsor.getPhone()));
        taxIdField.setText(safe(sponsor.getTax_id()));

        String existingLogo = safe(sponsor.getLogo_url());
        logoField.setText(existingLogo);
        selectedLogoFile = null; selectedDocFile = null; removeLogoRequested = false;

        if (!existingLogo.isBlank()) {
            try {
                Image img = new Image(existingLogo, true);
                img.errorProperty().addListener((obs, was, err) -> {
                    // Si l'image existante ne charge pas, chercher le logo
                    if (Boolean.TRUE.equals(err)) fetchLogoNow();
                });
                logoPreview.setImage(img);
                if (logoFileLabel != null) logoFileLabel.setText("Logo actuel chargé.");
            } catch (Exception e) {
                logoPreview.setImage(null);
                fetchLogoNow();
            }
        } else {
            // Pas de logo existant → chercher immédiatement
            fetchLogoNow();
        }

        if (docFileLabel         != null) docFileLabel.setText("Garder document actuel (ou choisir nouveau)");
        if (currencyComboBox     != null) currencyComboBox.setValue("TND");
        if (convertedAmountLabel != null) convertedAmountLabel.setText("");
    }

    public void preSelectEvent(Event event) {
        if (event != null) selectedEventId = event.getId();
    }

    // =========================================================================
    //  ✅ LOGO — DEBOUNCE + RECHERCHE STRICTE (pas de fallback alphabet)
    // =========================================================================

    /**
     * Planifie une recherche de logo après DEBOUNCE_MS ms d'inactivité.
     * Annule toute recherche précédente en cours.
     * → Évite de lancer 30 recherches pendant que l'utilisateur tape "Ooredoo".
     */
    private void scheduleLogoSearch() {
        if (removeLogoRequested || selectedLogoFile != null) return;

        // Annuler le timer précédent
        if (debounceTimer != null) {
            debounceTimer.cancel();
            debounceTimer = null;
        }

        String company = safe(companyField == null ? null : companyField.getText());

        // Ne pas chercher si le nom est trop court
        if (company.length() < MIN_COMPANY_LENGTH) {
            if (logoFileLabel != null)
                Platform.runLater(() -> logoFileLabel.setText("Saisissez le nom de l'entreprise pour charger son logo."));
            return;
        }

        // Afficher indicateur de chargement
        if (logoFileLabel != null)
            Platform.runLater(() -> logoFileLabel.setText("⏳ En attente…"));

        // Lancer après le délai debounce
        debounceTimer = new Timer(true); // daemon timer
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fetchLogoNow();
            }
        }, DEBOUNCE_MS);
    }

    /**
     * Lance la recherche du logo immédiatement (sans debounce).
     * À utiliser pour setModeEdit() ou setFixedEmail().
     */
    private void fetchLogoNow() {
        if (removeLogoRequested || selectedLogoFile != null) return;

        String company = safe(companyField == null ? null : companyField.getText());
        String email   = fixedEmail != null ? fixedEmail
                : safe(emailField == null ? null : emailField.getText());
        boolean hasLogoProvider = !resolveLogoDevImageToken().isBlank()
                || !resolveConfig("BRANDFETCH_CLIENT_ID").isBlank();

        if (company.isBlank() && email.isBlank()) return;

        // Indicateur
        Platform.runLater(() -> {
            if (logoFileLabel != null) logoFileLabel.setText("🔍 Recherche du logo officiel…");
        });

        new Thread(() -> {
            String logoUrl = findRealLogoUrl(company, email);
            Platform.runLater(() -> {
                if (logoUrl.isBlank()) {
                    // Aucun logo trouvé — on ne génère RIEN
                    logoField.clear();
                    logoPreview.setImage(null);
                    if (logoFileLabel != null) {
                        if (!hasLogoProvider) {
                            logoFileLabel.setText("Configurez LOGO_DEV_TOKEN (ex: -DLOGO_DEV_TOKEN=sk_...) ou BRANDFETCH_CLIENT_ID.");
                        } else {
                            logoFileLabel.setText("⚠️ Logo non trouvé. Vous pouvez en uploader un manuellement.");
                        }
                    }
                } else {
                    try {
                        Image img = new Image(logoUrl, true);
                        img.progressProperty().addListener((obs, old, progress) -> {
                            if (progress.doubleValue() >= 1.0 && !img.isError()) {
                                logoPreview.setImage(img);
                                logoField.setText(logoUrl);
                                if (logoFileLabel != null)
                                    logoFileLabel.setText("✅ Logo officiel chargé automatiquement.");
                            }
                        });
                        img.errorProperty().addListener((obs, was, err) -> {
                            if (Boolean.TRUE.equals(err)) {
                                logoField.clear();
                                logoPreview.setImage(null);
                                if (logoFileLabel != null)
                                    logoFileLabel.setText("⚠️ Logo introuvable. Uploadez-en un manuellement.");
                            }
                        });
                    } catch (Exception ex) {
                        logoField.clear();
                        logoPreview.setImage(null);
                        if (logoFileLabel != null)
                            logoFileLabel.setText("⚠️ Erreur chargement logo.");
                    }
                }
            });
        }, "logo-fetch").start();
    }

    /**
     * Cherche un logo reel d'entreprise via APIs externes actives.
     * Aucun fallback alphabet: toutes les requetes utilisent fallback=404.
     */
    private String findRealLogoUrl(String company, String email) {
        String logoDevToken = resolveLogoDevImageToken();
        String brandfetchClientId = resolveConfig("BRANDFETCH_CLIENT_ID");
        List<String> domains = buildDomainCandidates(company, email);
        for (String domain : domains) {
            if (!logoDevToken.isBlank()) {
                String logoDev = "https://img.logo.dev/" + domain
                        + "?token=" + urlEncode(logoDevToken)
                        + "&size=512&format=png&retina=true&fallback=404";
                if (isReachable(logoDev)) {
                    System.out.println("Logo Logo.dev trouve : " + logoDev);
                    return logoDev;
                }
            }

            if (!brandfetchClientId.isBlank()) {
                String brandfetch = "https://cdn.brandfetch.io/" + domain
                        + "/w/512/h/512/fallback/404/type/icon?c=" + urlEncode(brandfetchClientId);
                if (isReachable(brandfetch)) {
                    System.out.println("Logo Brandfetch trouve : " + brandfetch);
                    return brandfetch;
                }
            }
        }

        if (!logoDevToken.isBlank() && !safe(company).isBlank()) {
            String byName = "https://img.logo.dev/name/" + urlEncode(company)
                    + "?token=" + urlEncode(logoDevToken)
                    + "&size=512&format=png&retina=true&fallback=404";
            if (isReachable(byName)) {
                System.out.println("Logo Logo.dev (name) trouve : " + byName);
                return byName;
            }
        }

        System.out.println("Aucun logo officiel trouve : company='" + company + "' email='" + email + "'");
        return "";
    }

    /**
     * Construit les domaines candidats dans l'ordre de priorité.
     *
     * RÈGLE STRICTE :
     * - On utilise la Map KNOWN_BRAND_DOMAINS uniquement si le nom normalisé
     *   correspond EXACTEMENT à une clé (pas de contains partiel).
     * - Le domaine de l'email n'est utilisé que si ce n'est pas un email générique
     *   (gmail, yahoo, hotmail, outlook…).
     */
    private List<String> buildDomainCandidates(String company, String email) {
        Set<String> out = new LinkedHashSet<>();

        // 1. Correspondance EXACTE dans la map connue
        String knownDomain = resolveKnownDomainExact(company);
        if (!knownDomain.isBlank()) out.add(knownDomain);

        // 2. Domaine de l'email (si professionnel)
        String emailDomain = extractDomain(email);
        if (!emailDomain.isBlank() && isBusinessEmail(emailDomain)) {
            out.add(emailDomain);
        }

        // 3. Dérivé du nom de l'entreprise
        String normalized = normalizeCompany(company).replace(" ", "");
        if (!normalized.isBlank() && normalized.length() >= MIN_COMPANY_LENGTH) {
            out.add(normalized + ".com");
            out.add(normalized + ".tn");
            out.add(normalized + ".fr");
        }

        // 4. Slug avec tirets (ex: "tunisie-telecom.tn")
        String slug = normalizeCompany(company).replace(" ", "-");
        if (!slug.isBlank() && !slug.equals(normalized) && slug.length() >= MIN_COMPANY_LENGTH) {
            out.add(slug + ".com");
            out.add(slug + ".tn");
        }

        return new ArrayList<>(out);
    }

    /**
     * ✅ Correspondance EXACTE — évite "orange" de matcher "orange mobile corp"
     * On compare le nom normalisé avec chaque clé de la map.
     * On accepte une correspondance si la clé est égale au nom normalisé
     * OU si le nom normalisé commence exactement par la clé suivie d'un espace/fin.
     */
    private String resolveKnownDomainExact(String company) {
        String normalized = normalizeCompany(company);
        if (normalized.isBlank()) return "";

        // Correspondance exacte en priorité
        if (KNOWN_BRAND_DOMAINS.containsKey(normalized))
            return KNOWN_BRAND_DOMAINS.get(normalized);

        // Correspondance si le nom tapé est exactement une des clés connues
        for (Map.Entry<String, String> entry : KNOWN_BRAND_DOMAINS.entrySet()) {
            String key = entry.getKey();
            // Le nom normalisé DOIT être égal à la clé, ou commencer par "clé " (avec espace)
            if (normalized.equals(key) || normalized.startsWith(key + " ") || normalized.endsWith(" " + key)) {
                return entry.getValue();
            }
        }
        return "";
    }

    /** Retourne false pour les boîtes mail personnelles (gmail, yahoo, etc.) */
    private boolean isBusinessEmail(String domain) {
        String d = domain.toLowerCase();
        return !d.equals("gmail.com")   && !d.equals("yahoo.com")
                && !d.equals("hotmail.com") && !d.equals("outlook.com")
                && !d.equals("icloud.com")  && !d.equals("live.com")
                && !d.equals("msn.com")     && !d.equals("yahoo.fr")
                && !d.equals("hotmail.fr")  && !d.equals("laposte.net")
                && !d.equals("wanadoo.fr");
    }

    /** Vérifie si l'URL répond HTTP 2xx/3xx */
    private boolean isReachable(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "EventFlow/1.0")
                    .GET().build();
            HttpResponse<Void> resp = HTTP.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() >= 200 && resp.statusCode() < 400;
        } catch (Exception e) { return false; }
    }

    private String resolveConfig(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) return env.trim();
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) return prop.trim();
        String local = LOCAL_CONFIG.getProperty(key);
        if (local != null && !local.isBlank()) return local.trim();
        return "";
    }

    private String resolveLogoDevImageToken() {
        String token = resolveConfig("LOGO_DEV_PUBLISHABLE_KEY");
        if (token.isBlank()) {
            token = resolveConfig("LOGO_DEV_TOKEN");
        }
        if (token.startsWith("sk_")) {
            System.out.println("Logo.dev: cle sk_ detectee. Utilisez la cle publishable pk_ pour img.logo.dev.");
            return "";
        }
        return token;
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Properties loadLocalConfig() {
        Properties props = new Properties();
        Path homeConfig = Paths.get(System.getProperty("user.home"), ".eventflow", "secrets.properties");
        Path projectConfig = Paths.get(System.getProperty("user.dir"), "config", "local-secrets.properties");
        loadPropertiesIfExists(props, homeConfig);
        loadPropertiesIfExists(props, projectConfig);
        return props;
    }

    private static void loadPropertiesIfExists(Properties target, Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return;
        }
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            Properties tmp = new Properties();
            tmp.load(in);
            target.putAll(tmp);
        } catch (IOException ignored) {
        }
    }

    // =========================================================================
    //  ACTIONS FXML — LOGO / DOCUMENT
    // =========================================================================

    @FXML
    private void onChooseLogo() {
        clearErrors();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un logo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.webp","*.svg")
        );
        File file = chooser.showOpenDialog(getStage());
        if (file == null) return;

        selectedLogoFile = file;
        removeLogoRequested = false;
        if (logoFileLabel != null) logoFileLabel.setText(file.getName());
        try {
            logoPreview.setImage(new Image(file.toURI().toString(), true));
            logoField.clear();
        } catch (Exception ignored) {}
    }

    @FXML
    private void onRemoveLogo() {
        clearErrors();
        selectedLogoFile = null;
        removeLogoRequested = false;
        logoField.clear();
        logoPreview.setImage(null);
        if (logoFileLabel != null) logoFileLabel.setText("Logo supprimé.");
        // Re-chercher le logo officiel
        fetchLogoNow();
    }

    @FXML
    private void onChooseDocument() {
        clearErrors();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un justificatif");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg"),
                new FileChooser.ExtensionFilter("PDF",    "*.pdf")
        );
        File file = chooser.showOpenDialog(getStage());
        if (file != null) {
            selectedDocFile = file;
            if (docFileLabel != null) docFileLabel.setText(file.getName());
        }
    }

    // =========================================================================
    //  SAUVEGARDE
    // =========================================================================

    @FXML
    private void onSave() {
        clearErrors();

        // Annuler toute recherche de logo en cours
        if (debounceTimer != null) { debounceTimer.cancel(); debounceTimer = null; }

        int eventId;
        if (editing != null)             eventId = editing.getEvent_id();
        else if (selectedEventId != null) eventId = selectedEventId;
        else if (fixedEmail == null || fixedEmail.isBlank()) eventId = 0;
        else { error("Choisissez un événement via le bouton Sponsoriser."); return; }

        String company         = safe(companyField.getText());
        String email           = fixedEmail != null ? fixedEmail : safe(emailField.getText());
        String contributionTxt = safe(contributionField.getText()).replace(",", ".");
        String industry        = safe(industryField.getText());
        String phone           = safe(phoneField.getText());
        String taxId           = safe(taxIdField.getText());

        if (company.isEmpty())               { error("Entreprise obligatoire");                             return; }
        if (company.length() < 2)            { error("Entreprise trop courte");                             return; }
        if (company.matches(".*\\d.*"))      { error("Entreprise : pas de chiffres");                       return; }
        if (email.isEmpty())                 { error("Email obligatoire");                                   return; }
        if (!EMAIL_RX.matcher(email).matches()) { error("Email invalide");                                  return; }
        if (!taxId.isEmpty() && !TAX_ID_RX.matcher(taxId).matches())
        { error("Format N° Fiscal invalide (7 chiffres + 1 lettre, ex: 1234567A)");                     return; }

        String currency = currencyComboBox == null ? "TND" : currencyComboBox.getValue();
        double originalAmount;
        try {
            originalAmount = Double.parseDouble(contributionTxt);
            if (originalAmount <= 0) { error("Contribution doit être > 0"); return; }
        } catch (NumberFormatException e) { error("Contribution invalide (ex: 5000.00)"); return; }

        double contributionTnd = (currency == null || "TND".equals(currency))
                ? originalAmount : CurrencyService.convert(originalAmount, currency, "TND");
        if (contributionTnd < 0) { error("Erreur de conversion de devise."); return; }

        String logoUrlFinal = removeLogoRequested ? "" : safe(logoField.getText());
        try {
            if (!removeLogoRequested && selectedLogoFile != null) {
                logoUrlFinal = cloud.uploadLogo(selectedLogoFile);
                logoField.setText(logoUrlFinal);
            }
        } catch (Exception ex) { error("Upload logo échoué : " + ex.getMessage()); return; }

        String docUrlFinal = editing == null ? null : editing.getDocument_url();
        try {
            if (selectedDocFile != null) docUrlFinal = cloud.uploadPdfContract(selectedDocFile);
        } catch (Exception ex) { error("Upload document échoué : " + ex.getMessage()); return; }

        Sponsor out = new Sponsor();
        if (editing != null) out.setId(editing.getId());
        out.setEvent_id(eventId);
        out.setCompany_name(company);
        out.setContact_email(email);
        out.setContribution_name(contributionTnd);
        out.setLogo_url(logoUrlFinal.isEmpty() ? null : logoUrlFinal);
        out.setIndustry(industry);
        out.setPhone(phone);
        out.setTax_id(taxId);
        out.setDocument_url(docUrlFinal);
        out.setContract_url(editing == null ? null : editing.getContract_url());
        out.setAccess_code(editing  == null ? null : editing.getAccess_code());
        out.setUser_id(editing      == null ? null : editing.getUser_id());

        try {
            if (editing == null) sponsorService.addSponsor(out);
            else                 sponsorService.updateSponsor(out);

            Sponsor saved = sponsorService.getSponsorById(out.getId());
            result = saved != null ? saved : out;

            if (!phone.isEmpty()) {
                final String fc = company;
                final double fv = contributionTnd;
                new Thread(() -> {
                    boolean sent = WhatsAppService.sendConfirmation(phone, fc, fv);
                    Platform.runLater(() -> {
                        if (sent)
                            showInfo("WhatsApp", "Message envoyé à " + phone);
                        else {
                            String reason = WhatsAppService.getLastError();
                            showWarning("WhatsApp indisponible",
                                    "Message non envoyé.\n" +
                                            (reason == null || reason.isBlank()
                                                    ? "Vérifiez la configuration Twilio."
                                                    : reason));
                        }
                    });
                }).start();
            }

            showSuccess("Succès", "Sponsor enregistré avec succès.");
            if (onSaved    != null) onSaved.accept(result);
            if (onFormDone != null) onFormDone.run();
            closeWindowIfModal();

        } catch (Exception ex) { error("Erreur sauvegarde : " + ex.getMessage()); }
    }

    @FXML
    private void onCancel() {
        if (debounceTimer != null) { debounceTimer.cancel(); debounceTimer = null; }
        if (onFormDone != null) onFormDone.run();
        else closeWindowIfModal();
    }

    // =========================================================================
    //  UTILITAIRES
    // =========================================================================

    private void validateTaxId() {
        String taxId = safe(taxIdField.getText());
        if (!taxId.isEmpty() && !TAX_ID_RX.matcher(taxId).matches()) {
            taxIdField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
            error("Format N° Fiscal invalide (ex: 1234567A)");
        } else {
            taxIdField.setStyle("-fx-border-color: #cbd5e1; -fx-border-width: 1.5;");
            clearErrors();
        }
    }

    private void updateConvertedAmount() {
        if (currencyComboBox == null || contributionField == null || convertedAmountLabel == null) return;
        String currency   = currencyComboBox.getValue();
        String amountText = contributionField.getText().trim().replace(",", ".");
        if (amountText.isEmpty()) { convertedAmountLabel.setText(""); return; }
        try {
            double amount = Double.parseDouble(amountText);
            if (currency == null || "TND".equals(currency))
                convertedAmountLabel.setText(String.format("= %,.2f TND", amount));
            else {
                double c = CurrencyService.convert(amount, currency, "TND");
                convertedAmountLabel.setText(c >= 0 ? String.format("≈ %,.2f TND", c) : "Erreur conversion");
            }
        } catch (NumberFormatException e) { convertedAmountLabel.setText("Montant invalide"); }
    }

    private void applyEmailLockState() {
        if (emailField == null) return;
        boolean locked = fixedEmail != null && !fixedEmail.isBlank();
        emailField.setDisable(false);
        emailField.setEditable(!locked);
        emailField.setFocusTraversable(!locked);
    }

    private String normalizeCompany(String company) {
        String v = safe(company).toLowerCase();
        if (v.isBlank()) return "";
        String ascii = Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String extractDomain(String email) {
        String v = safe(email).toLowerCase();
        int at = v.lastIndexOf('@');
        if (at <= 0 || at >= v.length() - 1) return "";
        String domain = v.substring(at + 1).trim();
        return (domain.isEmpty() || !domain.contains(".")) ? "" : domain;
    }

    private void closeWindowIfModal() {
        if (onSaved != null) return;
        try {
            Stage s = getStage();
            if (s != null && s.isShowing()) s.close();
        } catch (Exception ignored) {}
    }

    private Stage getStage() {
        try { return (Stage) companyField.getScene().getWindow(); }
        catch (Exception e) { return null; }
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
    private void error(String msg)    { if (errorLabel != null) errorLabel.setText("Erreur : " + msg); }
    private void clearErrors()        { if (errorLabel != null) errorLabel.setText(""); }

    private void showInfo(String t, String m)    { alert(Alert.AlertType.INFORMATION, t, m); }
    private void showWarning(String t, String m) { alert(Alert.AlertType.WARNING,     t, m); }
    private void showSuccess(String t, String m) { alert(Alert.AlertType.INFORMATION, t, m); }

    private void alert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }
}