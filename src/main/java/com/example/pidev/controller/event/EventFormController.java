package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.model.weather.WeatherData;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventCategoryService;
import com.example.pidev.service.weather.WeatherService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;

/**
 * Controller pour le formulaire d'ajout/modification d'événement
 *
 * @author Ons Abdesslem
 * @version 4.0 - Gouvernorat → Faculté directe (sans Ville)
 */
public class EventFormController {

    // ==================== FXML ELEMENTS ====================
    @FXML private Label titleLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private DatePicker endDatePicker;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinuteSpinner;
    @FXML private ComboBox<String> gouvernoratCombo;
    // ✅ villeCombo supprimé du FXML — on garde le champ Java pour compatibilité setEvent()
    @FXML private ComboBox<String> locationCombo;
    @FXML private TextField capacityField;
    @FXML private TextField imageUrlField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private CheckBox freeCheckbox;
    @FXML private TextField priceField;
    @FXML private Button backBtn;
    @FXML private Button saveBtn;

    // Labels d'erreur
    @FXML private Label titleError;
    @FXML private Label descriptionError;
    @FXML private Label descriptionCounter;
    @FXML private Label dateError;
    @FXML private Label gouvernoratError;
    @FXML private Label locationError;
    @FXML private Label capacityError;
    @FXML private Label categoryError;
    @FXML private Label priceError;

    // ==================== AFFICHE IA ====================
    @FXML private StackPane affichePreview;
    @FXML private Button btnGenererAffiche;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label afficheStatus;

    // ==================== MÉTÉO ====================
    @FXML private VBox weatherContainer;
    @FXML private Label formWeatherEmoji;
    @FXML private Label formWeatherTemp;
    @FXML private Label formWeatherDescription;
    @FXML private Label formWeatherRain;
    @FXML private Label formWeatherHumidity;
    @FXML private Label formWeatherAlert;
    @FXML private Label formWeatherError;

    // ==================== NAVBAR ====================
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    // ==================== SERVICES ====================
    private EventService eventService;
    private EventCategoryService categoryService;
    private WeatherService weatherService;
    private MainController helloController;
    private Event currentEvent;
    private List<EventCategory> allCategories;

    // ==================== DONNÉES TUNISIENNES ====================
    private Map<String, List<String>> villesParGouvernorat;
    // Map : ville → liste facultés
    private Map<String, List<String>> facultesParVille;

    // ==================== VALIDATION CONSTANTS ====================
    private static final int TITLE_MIN_LENGTH = 5;
    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESC_MIN_LENGTH = 10;
    private static final int DESC_MAX_LENGTH = 1000;
    private static final int LOCATION_MIN_LENGTH = 3;
    private static final int LOCATION_MAX_LENGTH = 100;

    // ==================== PRISTINE FLAGS ====================
    private boolean titlePristine = true;
    private boolean descriptionPristine = true;
    private boolean startDatePristine = true;
    private boolean endDatePristine = true;
    private boolean locationPristine = true;
    private boolean capacityPristine = true;
    private boolean categoryPristine = true;
    private boolean pricePristine = true;

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("✅ EventFormController v4.0 initialisé");

        eventService = new EventService();
        categoryService = new EventCategoryService();
        weatherService = new WeatherService();

        loadTunisianData();
        loadGouvernorats();
        loadCategories();
        setupValidationListeners();
        setupLocationListeners();
        setDefaultValues();
        setupWeatherListeners();

        saveBtn.setDisable(true);

        updateDateTime();
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateDateTime()),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        if (dateLabel != null) {
            String dateText = now.format(dateFormatter);
            dateLabel.setText(dateText.substring(0, 1).toUpperCase() + dateText.substring(1));
        }
        if (timeLabel != null) timeLabel.setText(now.format(timeFormatter));
    }

    public void setMainController(MainController helloController) {
        this.helloController = helloController;
    }

    public void setEvent(Event event) {
        this.currentEvent = event;

        if (event != null) {
            titleLabel.setText("Modifier l'Événement");
            saveBtn.setText("💾 Mettre à jour");

            titleField.setText(event.getTitle());
            descriptionArea.setText(event.getDescription());

            if (event.getStartDate() != null) {
                startDatePicker.setValue(event.getStartDate().toLocalDate());
                startHourSpinner.getValueFactory().setValue(event.getStartDate().getHour());
                startMinuteSpinner.getValueFactory().setValue(event.getStartDate().getMinute());
            }
            if (event.getEndDate() != null) {
                endDatePicker.setValue(event.getEndDate().toLocalDate());
                endHourSpinner.getValueFactory().setValue(event.getEndDate().getHour());
                endMinuteSpinner.getValueFactory().setValue(event.getEndDate().getMinute());
            }

            // ✅ Gouvernorat → charge les facultés directement
            if (event.getGouvernorat() != null) {
                gouvernoratCombo.setValue(event.getGouvernorat());
                loadFacultesForGouvernorat(event.getGouvernorat());
            }

            // ✅ Restaurer le lieu sauvegardé
            if (event.getLocation() != null) {
                if (locationCombo.getItems().contains(event.getLocation())) {
                    locationCombo.setValue(event.getLocation());
                } else {
                    locationCombo.getEditor().setText(event.getLocation());
                }
            }

            capacityField.setText(String.valueOf(event.getCapacity()));
            imageUrlField.setText(event.getImageUrl());

            EventCategory cat = findCategoryById(event.getCategoryId());
            if (cat != null) categoryCombo.setValue(cat.getName());

            freeCheckbox.setSelected(event.isFree());
            priceField.setText(String.valueOf(event.getTicketPrice()));
            priceField.setDisable(event.isFree());

            resetPristineFlags();
            System.out.println("✏️ Mode modification: " + event.getTitle());
        } else {
            titleLabel.setText("Nouvel Événement");
            saveBtn.setText("💾 Enregistrer");
            System.out.println("➕ Mode création");
        }

        validateForm();
    }

    // ==================== SETUP ====================

    private void loadCategories() {
        try {
            allCategories = categoryService.getAllCategories();
            categoryCombo.getItems().clear();
            if (allCategories != null)
                for (EventCategory cat : allCategories)
                    categoryCombo.getItems().add(cat.getName());
            configureCategoryComboCells();
            System.out.println("✅ " + (allCategories != null ? allCategories.size() : 0) + " catégories chargées");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement catégories: " + e.getMessage());
        }
    }

    private void configureCategoryComboCells() {
        categoryCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                EventCategory cat = findCategoryByName(item);
                if (cat != null) {
                    Label iconLabel = new Label(cat.getIcon() != null ? cat.getIcon() : "📌");
                    iconLabel.setStyle("-fx-font-size: 16px;");
                    HBox box = new HBox(10, iconLabel, new Label(cat.getName()));
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box); setText(null);
                } else setText(item);
            }
        });
        categoryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                EventCategory cat = findCategoryByName(item);
                setText(cat != null ? cat.getIcon() + " " + cat.getName() : item);
            }
        });
    }

    private EventCategory findCategoryByName(String name) {
        if (allCategories != null)
            for (EventCategory cat : allCategories)
                if (cat.getName().equals(name)) return cat;
        return null;
    }

    private EventCategory findCategoryById(int id) {
        if (allCategories != null)
            for (EventCategory cat : allCategories)
                if (cat.getId() == id) return cat;
        return null;
    }

    private void resetPristineFlags() {
        titlePristine = true; descriptionPristine = true;
        startDatePristine = true; endDatePristine = true;
        locationPristine = true; capacityPristine = true;
        categoryPristine = true; pricePristine = true;
    }

    private void setupValidationListeners() {
        titleField.textProperty().addListener((obs, o, n) -> { titlePristine = false; validateForm(); });
        titleField.focusedProperty().addListener((obs, o, n) -> { if (!n && titleField.getText().trim().isEmpty()) { titlePristine = false; validateForm(); } });

        descriptionArea.textProperty().addListener((obs, o, n) -> { descriptionPristine = false; updateDescriptionCounter(); validateForm(); });
        descriptionArea.focusedProperty().addListener((obs, o, n) -> { if (!n && descriptionArea.getText().trim().isEmpty()) { descriptionPristine = false; validateForm(); } });

        startDatePicker.valueProperty().addListener((obs, o, n) -> { startDatePristine = false; validateForm(); });
        startDatePicker.focusedProperty().addListener((obs, o, n) -> { if (!n && startDatePicker.getValue() == null) { startDatePristine = false; validateForm(); } });
        endDatePicker.valueProperty().addListener((obs, o, n) -> { endDatePristine = false; validateForm(); });
        endDatePicker.focusedProperty().addListener((obs, o, n) -> { if (!n && endDatePicker.getValue() == null) { endDatePristine = false; validateForm(); } });

        startHourSpinner.valueProperty().addListener((obs, o, n) -> { startDatePristine = false; validateForm(); });
        startMinuteSpinner.valueProperty().addListener((obs, o, n) -> { startDatePristine = false; validateForm(); });
        endHourSpinner.valueProperty().addListener((obs, o, n) -> { endDatePristine = false; validateForm(); });
        endMinuteSpinner.valueProperty().addListener((obs, o, n) -> { endDatePristine = false; validateForm(); });

        gouvernoratCombo.valueProperty().addListener((obs, o, n) -> { if (n != null) { locationPristine = false; validateForm(); } });
        locationCombo.valueProperty().addListener((obs, o, n) -> { locationPristine = false; validateForm(); });
        locationCombo.getEditor().textProperty().addListener((obs, o, n) -> { locationPristine = false; validateForm(); });

        capacityField.textProperty().addListener((obs, o, n) -> {
            capacityPristine = false;
            if (!n.matches("\\d*")) capacityField.setText(n.replaceAll("[^\\d]", ""));
            validateForm();
        });
        capacityField.focusedProperty().addListener((obs, o, n) -> { if (!n && capacityField.getText().trim().isEmpty()) { capacityPristine = false; validateForm(); } });

        categoryCombo.valueProperty().addListener((obs, o, n) -> { categoryPristine = false; validateForm(); });

        freeCheckbox.selectedProperty().addListener((obs, o, isFree) -> {
            priceField.setDisable(isFree);
            if (isFree) { priceField.setText("0"); priceError.setVisible(false); clearFieldStyles(priceField); }
            else pricePristine = false;
            validateForm();
        });

        priceField.textProperty().addListener((obs, o, n) -> {
            pricePristine = false;
            if (!n.matches("\\d*\\.?\\d*")) priceField.setText(o);
            validateForm();
        });
        priceField.focusedProperty().addListener((obs, o, n) -> { if (!n && !freeCheckbox.isSelected() && priceField.getText().trim().isEmpty()) { pricePristine = false; validateForm(); } });
    }

    private void updateDescriptionCounter() {
        if (descriptionCounter != null) {
            int length = descriptionArea.getText().length();
            descriptionCounter.setText(length + "/" + DESC_MAX_LENGTH);
            if (length > DESC_MAX_LENGTH) descriptionCounter.setStyle("-fx-text-fill: #ef4444;");
            else if (length >= DESC_MAX_LENGTH - 100) descriptionCounter.setStyle("-fx-text-fill: #f59e0b;");
            else descriptionCounter.setStyle("-fx-text-fill: #6c757d;");
        }
    }

    private void setDefaultValues() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        startHourSpinner.getValueFactory().setValue(9);
        startMinuteSpinner.getValueFactory().setValue(0);
        endHourSpinner.getValueFactory().setValue(17);
        endMinuteSpinner.getValueFactory().setValue(0);
        capacityField.setText("50");
        freeCheckbox.setSelected(true);
        priceField.setDisable(true);
        priceField.setText("0");
        if (!categoryCombo.getItems().isEmpty()) categoryCombo.setValue(categoryCombo.getItems().get(0));
    }

    // ==================== MÉTÉO ====================

    private void setupWeatherListeners() {
        // ✅ Météo basée sur le gouvernorat maintenant (plus de villeCombo)
        gouvernoratCombo.valueProperty().addListener((obs, o, n) -> loadWeatherPreview());
        startDatePicker.valueProperty().addListener((obs, o, n) -> loadWeatherPreview());
    }

    private void loadWeatherPreview() {
        String gouvernorat = gouvernoratCombo.getValue();
        LocalDate date = startDatePicker.getValue();
        if (gouvernorat == null || gouvernorat.isEmpty() || date == null) { hideWeatherContainer(); return; }

        Thread weatherThread = new Thread(() -> {
            try {
                Event tempEvent = new Event();
                tempEvent.setLocation(gouvernorat);
                tempEvent.setStartDate(LocalDateTime.of(date, LocalTime.of(9, 0)));
                WeatherData weather = weatherService.getWeatherForEvent(tempEvent);
                Platform.runLater(() -> {
                    if (weather != null && weather.isAvailable()) displayWeatherPreview(weather);
                    else showWeatherError(weather != null ? weather.getErrorMessage() : "Météo indisponible");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showWeatherError("Erreur lors du chargement de la météo"));
            }
        });
        weatherThread.setDaemon(true);
        weatherThread.start();
    }

    private void displayWeatherPreview(WeatherData weather) {
        if (weatherContainer == null) return;
        try {
            weatherContainer.setVisible(true); weatherContainer.setManaged(true);
            formWeatherEmoji.setText(weather.getWeatherEmoji());
            formWeatherTemp.setText(String.format("%.1f°C", weather.getTemperature()));
            formWeatherDescription.setText(weather.getDescription());
            formWeatherRain.setText(weather.getRainChance() + "%");
            formWeatherHumidity.setText(String.format("%.0f%%", weather.getHumidity()));
            formWeatherError.setVisible(false); formWeatherError.setManaged(false);
            if (weather.getRainChance() > 60) {
                formWeatherAlert.setText("Risque de pluie eleve! Prevoyez une salle couverte ou un plan B");
                formWeatherAlert.setVisible(true); formWeatherAlert.setManaged(true);
            } else { formWeatherAlert.setVisible(false); formWeatherAlert.setManaged(false); }
        } catch (Exception e) { showWeatherError("Erreur affichage météo"); }
    }

    private void showWeatherError(String errorMsg) {
        if (weatherContainer == null) return;
        weatherContainer.setVisible(true); weatherContainer.setManaged(true);
        formWeatherError.setText("Erreur: " + errorMsg);
        formWeatherError.setVisible(true); formWeatherError.setManaged(true);
        formWeatherAlert.setVisible(false); formWeatherAlert.setManaged(false);
    }

    private void hideWeatherContainer() {
        if (weatherContainer != null) { weatherContainer.setVisible(false); weatherContainer.setManaged(false); }
    }

    // ==================== VALIDATION ====================

    private boolean validateForm() {
        boolean isValid = true;
        resetValidationUI();

        if (!titlePristine) {
            String title = titleField.getText().trim();
            if (title.isEmpty()) { titleError.setText("Le titre est requis"); titleError.setVisible(true); applyErrorStyle(titleField); isValid = false; }
            else if (title.length() < TITLE_MIN_LENGTH) { titleError.setText("Min " + TITLE_MIN_LENGTH + " caracteres"); titleError.setVisible(true); applyErrorStyle(titleField); isValid = false; }
            else if (title.length() > TITLE_MAX_LENGTH) { titleError.setText("Max " + TITLE_MAX_LENGTH + " caracteres"); titleError.setVisible(true); applyErrorStyle(titleField); isValid = false; }
            else applySuccessStyle(titleField);
        }

        if (!descriptionPristine) {
            String desc = descriptionArea.getText().trim();
            if (desc.isEmpty()) { descriptionError.setText("La description est requise"); descriptionError.setVisible(true); applyErrorStyle(descriptionArea); isValid = false; }
            else if (desc.length() < DESC_MIN_LENGTH) { descriptionError.setText("Min " + DESC_MIN_LENGTH + " caracteres"); descriptionError.setVisible(true); applyErrorStyle(descriptionArea); isValid = false; }
            else if (desc.length() > DESC_MAX_LENGTH) { descriptionError.setText("Max " + DESC_MAX_LENGTH + " caracteres"); descriptionError.setVisible(true); applyErrorStyle(descriptionArea); isValid = false; }
            else applySuccessStyle(descriptionArea);
        }

        if (!startDatePristine || !endDatePristine) {
            if (startDatePicker.getValue() == null) { dateError.setText("La date de debut est requise"); dateError.setVisible(true); applyErrorStyle(startDatePicker); isValid = false; }
            else if (endDatePicker.getValue() == null) { dateError.setText("La date de fin est requise"); dateError.setVisible(true); applyErrorStyle(endDatePicker); isValid = false; }
            else {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startDT = LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue()));
                LocalDateTime endDT = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue()));
                if (currentEvent == null && startDT.isBefore(now)) { dateError.setText("La date ne peut pas etre dans le passe"); dateError.setVisible(true); applyErrorStyle(startDatePicker); isValid = false; }
                else if (endDT.isBefore(startDT) || endDT.isEqual(startDT)) { dateError.setText("La fin doit etre apres le debut"); dateError.setVisible(true); applyErrorStyle(endDatePicker); isValid = false; }
                else { applySuccessStyle(startDatePicker); applySuccessStyle(endDatePicker); }
            }
        }

        if (!locationPristine) {
            String location = locationCombo.getValue();
            if (location == null || location.isEmpty()) location = locationCombo.getEditor().getText().trim();
            if (location.isEmpty()) { locationError.setText("Le lieu est requis"); locationError.setVisible(true); applyErrorStyle(locationCombo); isValid = false; }
            else if (location.length() < LOCATION_MIN_LENGTH) { locationError.setText("Min " + LOCATION_MIN_LENGTH + " caracteres"); locationError.setVisible(true); applyErrorStyle(locationCombo); isValid = false; }
            else if (location.length() > LOCATION_MAX_LENGTH) { locationError.setText("Max " + LOCATION_MAX_LENGTH + " caracteres"); locationError.setVisible(true); applyErrorStyle(locationCombo); isValid = false; }
            else applySuccessStyle(locationCombo);
        }

        if (!capacityPristine) {
            String capacity = capacityField.getText().trim();
            if (capacity.isEmpty()) { capacityError.setText("La capacite est requise"); capacityError.setVisible(true); applyErrorStyle(capacityField); isValid = false; }
            else {
                try {
                    int cap = Integer.parseInt(capacity);
                    if (cap < 1) { capacityError.setText("Minimum 1 place"); capacityError.setVisible(true); applyErrorStyle(capacityField); isValid = false; }
                    else applySuccessStyle(capacityField);
                } catch (NumberFormatException e) { capacityError.setText("Doit etre un nombre entier"); capacityError.setVisible(true); applyErrorStyle(capacityField); isValid = false; }
            }
        }

        if (!categoryPristine) {
            if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) { categoryError.setText("La categorie est requise"); categoryError.setVisible(true); applyErrorStyle(categoryCombo); isValid = false; }
            else applySuccessStyle(categoryCombo);
        }

        if (!pricePristine && !freeCheckbox.isSelected()) {
            String price = priceField.getText().trim();
            if (price.isEmpty()) { priceError.setText("Le prix est requis si non gratuit"); priceError.setVisible(true); applyErrorStyle(priceField); isValid = false; }
            else {
                try {
                    double pv = Double.parseDouble(price);
                    if (pv < 0) { priceError.setText("Le prix ne peut pas etre negatif"); priceError.setVisible(true); applyErrorStyle(priceField); isValid = false; }
                    else applySuccessStyle(priceField);
                } catch (NumberFormatException e) { priceError.setText("Format invalide (ex: 25.50)"); priceError.setVisible(true); applyErrorStyle(priceField); isValid = false; }
            }
        }

        saveBtn.setDisable(!isValid);
        return isValid;
    }

    private void resetValidationUI() {
        titleError.setVisible(false); descriptionError.setVisible(false); dateError.setVisible(false);
        locationError.setVisible(false); capacityError.setVisible(false); categoryError.setVisible(false); priceError.setVisible(false);
        clearFieldStyles(titleField); clearFieldStyles(descriptionArea); clearFieldStyles(startDatePicker);
        clearFieldStyles(endDatePicker); clearFieldStyles(locationCombo); clearFieldStyles(capacityField);
        clearFieldStyles(categoryCombo); clearFieldStyles(priceField);
    }

    private void applyErrorStyle(Control f) { f.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-background-color: #fff5f5;"); }
    private void applySuccessStyle(Control f) { f.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-background-color: #f0fdf4;"); }
    private void clearFieldStyles(Control f) { f.setStyle("-fx-border-color: #ced4da; -fx-background-color: #f8f9fa;"); }

    // ==================== ACTIONS ====================

    @FXML private void handleBack() { if (helloController != null) helloController.showEventsList(); }

    @FXML
    private void handleCancel() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation"); a.setHeaderText("Annuler les modifications?");
        a.setContentText("Les modifications non enregistrees seront perdues.");
        a.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) handleBack(); });
    }

    // ==================== GÉNÉRATION AFFICHE IA ====================

    @FXML
    private void handleGenererAffiche() {
        final String titre = titleField != null ? titleField.getText().trim() : "";
        final String gouvernorat = gouvernoratCombo != null && gouvernoratCombo.getValue() != null ? gouvernoratCombo.getValue().trim() : "";
        final String categorie = categoryCombo != null && categoryCombo.getValue() != null ? categoryCombo.getValue().trim() : "";

        if (titre.isEmpty() || gouvernorat.isEmpty() || categorie.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Champs requis"); alert.setHeaderText(null);
            alert.setContentText("Merci de renseigner le titre, le gouvernorat et la categorie avant de generer l'affiche.");
            alert.showAndWait();
            return;
        }

        btnGenererAffiche.setDisable(true);
        loadingSpinner.setManaged(true); loadingSpinner.setVisible(true);
        afficheStatus.setText("Generation en cours...");

        Thread worker = new Thread(() -> {
            java.net.HttpURLConnection conn = null;
            java.io.InputStream inputStream = null;
            try {
                String prompt = String.format(
                        "professional university event poster, title: %s, category: %s, location: %s, modern design, vibrant colors, no text overlay",
                        titre, categorie, gouvernorat);

                java.net.URL urlObj = new java.net.URL("https://router.huggingface.co/hf-inference/models/black-forest-labs/FLUX.1-schnell");
                conn = (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                String hfToken = System.getenv("HF_TOKEN");
                conn.setRequestProperty("Authorization", "Bearer " + System.getenv("HF_TOKEN"));

                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "image/png");
                conn.setDoOutput(true);
                conn.setConnectTimeout(60000); conn.setReadTimeout(60000);

                String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"");
                String body = String.format("{\"inputs\":\"%s\",\"parameters\":{\"num_inference_steps\":4}}", safePrompt);
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); os.flush(); }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    String errorMsg = "Erreur inconnue";
                    java.io.InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder(); String line;
                            while ((line = reader.readLine()) != null) sb.append(line);
                            errorMsg = sb.toString();
                        }
                    }
                    throw new Exception("Erreur API Hugging Face " + responseCode + ": " + errorMsg);
                }

                inputStream = conn.getInputStream();
                final Image image = new Image(inputStream);
                if (image.isError()) throw new Exception("Impossible de décoder l'image reçue");

                Platform.runLater(() -> {
                    try {
                        affichePreview.getChildren().clear();
                        ImageView bg = new ImageView(image);
                        bg.setFitWidth(300); bg.setFitHeight(450);
                        bg.setPreserveRatio(false); bg.setSmooth(true);

                        final String dateStr = startDatePicker != null && startDatePicker.getValue() != null
                                ? startDatePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
                        final boolean gratuit = freeCheckbox != null && freeCheckbox.isSelected();
                        final String prixStr = gratuit ? "Gratuit" : (priceField != null ? priceField.getText().trim() + " DT" : "");

                        Label titreLbl = new Label(titre);
                        titreLbl.setWrapText(true);
                        titreLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
                        Label categorieLbl = new Label(categorie);
                        categorieLbl.setStyle("-fx-text-fill: #90CDF4; -fx-font-size: 12px;");
                        Label lieuLbl = new Label("📍 " + gouvernorat);
                        lieuLbl.setWrapText(true);
                        lieuLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 12px;");
                        Label dateLbl = new Label("📅 " + dateStr);
                        dateLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 12px;");
                        Label prixLbl = new Label("💰 " + prixStr);
                        prixLbl.setStyle(gratuit ? "-fx-text-fill: #68D391; -fx-font-size: 12px; -fx-font-weight: bold;" : "-fx-text-fill: #FBD38D; -fx-font-size: 12px; -fx-font-weight: bold;");

                        Region spacer = new Region();
                        VBox.setVgrow(spacer, Priority.ALWAYS);
                        VBox overlay = new VBox(6, spacer, titreLbl, categorieLbl, lieuLbl, dateLbl, prixLbl);
                        overlay.setAlignment(Pos.BOTTOM_LEFT);
                        overlay.setPadding(new Insets(16));
                        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
                        overlay.setPrefSize(300, 450); overlay.setMaxSize(300, 450);

                        affichePreview.getChildren().addAll(bg, overlay);
                        afficheStatus.setText("Affiche generee avec succes!");
                    } finally {
                        btnGenererAffiche.setDisable(false);
                        loadingSpinner.setVisible(false); loadingSpinner.setManaged(false);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnGenererAffiche.setDisable(false);
                    loadingSpinner.setVisible(false); loadingSpinner.setManaged(false);
                    afficheStatus.setText("Erreur: " + ex.getMessage());
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Generation echouee"); alert.setHeaderText(null);
                    alert.setContentText("Impossible de generer l'affiche: " + ex.getMessage());
                    alert.showAndWait();
                });
            } finally {
                try { if (inputStream != null) inputStream.close(); if (conn != null) conn.disconnect(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    // ==================== SNAPSHOT AFFICHE ====================

    private String saveAfficheToFile() {
        try {
            if (affichePreview == null || affichePreview.getChildren().isEmpty()) return null;
            File postersDir = new File("src/main/resources/assets/posters");
            if (!postersDir.exists()) postersDir.mkdirs();
            String fileName = "poster_" + System.currentTimeMillis() + ".png";
            File outputFile = new File(postersDir, fileName);
            WritableImage snapshot = affichePreview.snapshot(new SnapshotParameters(), null);
            if (snapshot == null) return null;
            int w = (int) snapshot.getWidth(), h = (int) snapshot.getHeight();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            if (!ImageIO.write(img, "png", outputFile)) return null;
            String path = "assets/posters/" + fileName;
            System.out.println("✅ Affiche sauvegardée: " + path);
            return path;
        } catch (IOException e) {
            System.err.println("❌ Erreur sauvegarde affiche: " + e.getMessage());
            return null;
        }
    }

    // ==================== SAVE ====================

    @FXML
    private void handleSave() {
        titlePristine = false; descriptionPristine = false;
        startDatePristine = false; endDatePristine = false;
        locationPristine = false; capacityPristine = false;
        categoryPristine = false; pricePristine = false;

        if (!validateForm()) return;

        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String gouvernorat = gouvernoratCombo.getValue();

        // ✅ ville = gouvernorat (on garde la compatibilité avec le modèle)
        String ville = gouvernorat;

        String location = locationCombo.getValue();
        if (location == null || location.isEmpty()) {
            location = locationCombo.getEditor().getText().trim();
            if (location.isEmpty()) { showError("Erreur", "Veuillez selectionner ou saisir un lieu"); return; }
        }

        int capacity = Integer.parseInt(capacityField.getText());
        String imageUrl = imageUrlField.getText().trim();

        String afficheUrl = saveAfficheToFile();
        if (afficheUrl != null) { imageUrl = afficheUrl; System.out.println("✅ Affiche utilisée comme image"); }

        EventCategory selectedCat = findCategoryByName(categoryCombo.getValue());
        int categoryId = selectedCat != null ? selectedCat.getId() : 1;

        boolean isFree = freeCheckbox.isSelected();
        double price = isFree ? 0 : Double.parseDouble(priceField.getText());

        LocalDateTime startDate = LocalDateTime.of(startDatePicker.getValue(), LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue()));
        LocalDateTime endDate = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue()));

        try {
            boolean success;
            if (currentEvent == null) {
                Event newEvent = new Event(title, description, startDate, endDate, location, capacity, imageUrl, categoryId, 1, isFree, price);
                newEvent.setGouvernorat(gouvernorat);
                newEvent.setVille(ville);
                success = eventService.addEvent(newEvent);
                if (success) {
                    showSuccess("Succes", "Evenement cree avec succes!");
                    if (helloController != null) helloController.refreshKPIs();
                    handleBack();
                } else showError("Erreur", "Impossible de creer l'evenement");
            } else {
                currentEvent.setTitle(title); currentEvent.setDescription(description);
                currentEvent.setStartDate(startDate); currentEvent.setEndDate(endDate);
                currentEvent.setLocation(location); currentEvent.setGouvernorat(gouvernorat);
                currentEvent.setVille(ville); currentEvent.setCapacity(capacity);
                currentEvent.setImageUrl(imageUrl); currentEvent.setCategoryId(categoryId);
                currentEvent.setFree(isFree); currentEvent.setTicketPrice(price);
                success = eventService.updateEvent(currentEvent);
                if (success) {
                    showSuccess("Succes", "Evenement mis a jour avec succes!");
                    if (helloController != null) { helloController.refreshKPIs(); helloController.refreshEventsFrontPage(); }
                    handleBack();
                } else showError("Erreur", "Impossible de mettre a jour l'evenement");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde: " + e.getMessage());
            showError("Erreur", "Une erreur est survenue: " + e.getMessage());
        }
    }

    // ==================== DIALOGS ====================
    private void showError(String title, String msg) { Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }
    private void showSuccess(String title, String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }

    // ==================== DONNÉES TUNISIENNES ====================

    private void loadTunisianData() {
        villesParGouvernorat = new LinkedHashMap<>();
        facultesParVille = new LinkedHashMap<>();

        // Villes par gouvernorat (gardées pour compatibilité setEvent)
        villesParGouvernorat.put("Tunis", Arrays.asList("Tunis", "La Marsa", "Carthage", "Le Bardo"));
        villesParGouvernorat.put("Ariana", Arrays.asList("Ariana", "Ettadhamen", "Raoued"));
        villesParGouvernorat.put("Ben Arous", Arrays.asList("Ben Arous", "Hammam Lif", "Rades"));
        villesParGouvernorat.put("Manouba", Arrays.asList("Manouba", "Oued Ellil", "Douar Hicher"));
        villesParGouvernorat.put("Nabeul", Arrays.asList("Nabeul", "Hammamet", "Grombalia"));
        villesParGouvernorat.put("Zaghouan", Arrays.asList("Zaghouan", "Bir Mcherga", "El Fahs"));
        villesParGouvernorat.put("Bizerte", Arrays.asList("Bizerte", "Menzel Bourguiba", "Mateur"));
        villesParGouvernorat.put("Béja", Arrays.asList("Béja", "Testour", "Medjez el-Bab"));
        villesParGouvernorat.put("Jendouba", Arrays.asList("Jendouba", "Tabarka", "Ain Draham"));
        villesParGouvernorat.put("Le Kef", Arrays.asList("Le Kef", "Dahmani", "Tajerouine"));
        villesParGouvernorat.put("Siliana", Arrays.asList("Siliana", "Maktar", "Bargou"));
        villesParGouvernorat.put("Kairouan", Arrays.asList("Kairouan", "Haffouz", "Sbikha"));
        villesParGouvernorat.put("Kasserine", Arrays.asList("Kasserine", "Sbeitla", "Feriana"));
        villesParGouvernorat.put("Sidi Bouzid", Arrays.asList("Sidi Bouzid", "Regueb", "Mezzouna"));
        villesParGouvernorat.put("Sousse", Arrays.asList("Sousse", "Msaken", "Hammam Sousse"));
        villesParGouvernorat.put("Monastir", Arrays.asList("Monastir", "Moknine", "Ksar Hellal"));
        villesParGouvernorat.put("Mahdia", Arrays.asList("Mahdia", "Ksour Essaf", "Chebba"));
        villesParGouvernorat.put("Sfax", Arrays.asList("Sfax", "Sakiet Eddaïer", "Sakiet Ezzit"));
        villesParGouvernorat.put("Gafsa", Arrays.asList("Gafsa", "Metlaoui", "Redeyef"));
        villesParGouvernorat.put("Tozeur", Arrays.asList("Tozeur", "Nefta", "Degache"));
        villesParGouvernorat.put("Kebili", Arrays.asList("Kebili", "Douz", "Souk Lahad"));
        villesParGouvernorat.put("Gabès", Arrays.asList("Gabès", "Mareth", "Matmata"));
        villesParGouvernorat.put("Medenine", Arrays.asList("Medenine", "Zarzis", "Djerba"));
        villesParGouvernorat.put("Tataouine", Arrays.asList("Tataouine", "Ghomrassen", "Remada"));

        // ✅ Facultés par ville (pour lookup interne)
        facultesParVille.put("Tunis", Arrays.asList("FST Tunis", "FSEG Tunis", "FD Tunis", "FLSH Tunis", "ENIT", "ESSEC", "IHEC", "ISG Tunis"));
        facultesParVille.put("Ariana", Arrays.asList("ESPRIT", "ESB", "ENSI", "SUP'COM", "ISI", "SESAME University"));
        facultesParVille.put("Manouba", Arrays.asList("ISAMM", "ISBST", "FSHS Manouba", "ISBAM", "ISCAE Manouba"));
        facultesParVille.put("Sousse", Arrays.asList("FSM Sousse", "ESSTHS", "ISITCom", "ISSAT Sousse", "IHEC Sousse"));
        facultesParVille.put("Sfax", Arrays.asList("FSS Sfax", "ENIS Sfax", "ISIMS", "FSEGS Sfax", "ISET Sfax"));
        facultesParVille.put("Monastir", Arrays.asList("FM Monastir", "FSM Monastir", "ISIM", "ISET Monastir"));
        facultesParVille.put("Bizerte", Arrays.asList("INSAT", "FSB Bizerte", "ISSAT Bizerte"));
        facultesParVille.put("Nabeul", Arrays.asList("ISSAT Nabeul", "ISET Nabeul"));
        facultesParVille.put("Kairouan", Arrays.asList("FSHS Kairouan", "ISET Kairouan"));
        facultesParVille.put("Gafsa", Arrays.asList("FSL Gafsa", "ISET Gafsa"));
        facultesParVille.put("Gabès", Arrays.asList("FSS Gabès", "ISSAT Gabès", "ISET Gabès"));

        System.out.println("✅ Données tunisiennes chargées");
    }

    private void loadGouvernorats() {
        gouvernoratCombo.getItems().clear();
        // ✅ Afficher seulement les gouvernorats qui ont au moins une ville avec des facultés
        for (String gouvernorat : villesParGouvernorat.keySet()) {
            List<String> villes = villesParGouvernorat.get(gouvernorat);
            boolean hasFaculte = villes.stream().anyMatch(v -> facultesParVille.containsKey(v));
            if (hasFaculte) gouvernoratCombo.getItems().add(gouvernorat);
        }
    }

    private void setupLocationListeners() {
        gouvernoratCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                locationCombo.setValue(null);
                locationCombo.getEditor().clear();
                loadFacultesForGouvernorat(newVal);
            }
        });
    }

    /**
     * ✅ NOUVELLE MÉTHODE : charge toutes les facultés du gouvernorat
     * (toutes ses villes confondues) directement dans locationCombo
     */
    private void loadFacultesForGouvernorat(String gouvernorat) {
        locationCombo.getItems().clear();
        List<String> villes = villesParGouvernorat.get(gouvernorat);
        if (villes != null) {
            for (String ville : villes) {
                List<String> facultes = facultesParVille.get(ville);
                if (facultes != null) locationCombo.getItems().addAll(facultes);
            }
        }
        if (locationCombo.getItems().isEmpty()) {
            locationCombo.setPromptText("Saisir le lieu manuellement");
        } else {
            locationCombo.setPromptText("Sélectionner une faculté");
        }
    }
}
