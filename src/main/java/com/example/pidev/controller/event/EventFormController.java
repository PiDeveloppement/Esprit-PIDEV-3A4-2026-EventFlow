package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.model.weather.WeatherData;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventCategoryService;
import com.example.pidev.service.weather.WeatherService;
import com.example.pidev.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller pour le formulaire d'ajout/modification d'événement
 * Structure identique à CategoryFormController
 *
 * @author Ons Abdesslem
 * @version 2.0 - Avec validation progressive
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
    @FXML private ComboBox<String> villeCombo;
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
    @FXML private Label villeError;
    @FXML private Label locationError;
    @FXML private Label capacityError;
    @FXML private Label categoryError;
    @FXML private Label priceError;

    // ==================== MÉTÉO ELEMENTS ====================
    @FXML private VBox weatherContainer;
    @FXML private Label formWeatherEmoji;
    @FXML private Label formWeatherTemp;
    @FXML private Label formWeatherDescription;
    @FXML private Label formWeatherRain;
    @FXML private Label formWeatherHumidity;
    @FXML private Label formWeatherAlert;
    @FXML private Label formWeatherError;

    // ==================== NAVBAR ELEMENTS ====================
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    // ==================== SERVICES ====================

    private EventService eventService;
    private EventCategoryService categoryService;
    private WeatherService weatherService;
    private MainController helloController;
    private Event currentEvent;
    private List<EventCategory> allCategories;

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
        System.out.println("✅ EventFormController initialisé");

        eventService = new EventService();
        categoryService = new EventCategoryService();
        weatherService = new WeatherService();

        loadTunisianData();
        loadGouvernorats();
        loadCategories();
        setupSpinners();
        setupValidationListeners();
        setupLocationListeners();
        setDefaultValues();
        setupWeatherListeners();

        // Initialiser l'état du bouton save
        saveBtn.setDisable(true);

        // Initialiser la date et l'heure
        updateDateTime();

        // Mettre à jour l'heure chaque seconde
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

        if (timeLabel != null) {
            timeLabel.setText(now.format(timeFormatter));
        }
    }

    public void setMainController(MainController helloController) {
        this.helloController = helloController;
    }

    public void setEvent(Event event) {
        this.currentEvent = event;

        if (event != null) {
            // Mode modification
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

            // Charger gouvernorat, ville et location
            if (event.getGouvernorat() != null) {
                gouvernoratCombo.setValue(event.getGouvernorat());
                loadVillesForGouvernorat(event.getGouvernorat());

                if (event.getVille() != null) {
                    villeCombo.setValue(event.getVille());
                    loadFacultesForVille(event.getVille());
                }
            }

            if (event.getLocation() != null) {
                // Essayer de sélectionner le location dans la liste
                if (locationCombo.getItems().contains(event.getLocation())) {
                    locationCombo.setValue(event.getLocation());
                } else {
                    // Sinon le saisir manuellement dans l'editor
                    locationCombo.getEditor().setText(event.getLocation());
                }
            }

            capacityField.setText(String.valueOf(event.getCapacity()));
            imageUrlField.setText(event.getImageUrl());

            EventCategory cat = findCategoryById(event.getCategoryId());
            if (cat != null) {
                categoryCombo.setValue(cat.getName());
            }

            freeCheckbox.setSelected(event.isFree());
            priceField.setText(String.valueOf(event.getTicketPrice()));
            priceField.setDisable(event.isFree());

            // Réinitialiser tous les flags pristine en mode édition
            resetPristineFlags();

            System.out.println("✏️ Mode modification: " + event.getTitle());
        } else {
            // Mode création
            titleLabel.setText("Nouvel Événement");
            saveBtn.setText("💾 Enregistrer");

            System.out.println("➕ Mode création");
        }

        // Valider le formulaire après le chargement
        validateForm();
    }

    // ==================== SETUP ====================

    private void loadCategories() {
        try {
            allCategories = categoryService.getAllCategories();
            categoryCombo.getItems().clear();

            if (allCategories != null) {
                for (EventCategory cat : allCategories) {
                    categoryCombo.getItems().add(cat.getName());
                }
            }

            configureCategoryComboCells();
            System.out.println("✅ " + (allCategories != null ? allCategories.size() : 0) + " catégories chargées");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement catégories: " + e.getMessage());
        }
    }

    private void setupSpinners() {
        // Les spinners sont déjà configurés dans le FXML avec valueFactory
    }

    private void configureCategoryComboCells() {
        categoryCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                EventCategory cat = findCategoryByName(item);
                if (cat != null) {
                    Label iconLabel = new Label(cat.getIcon() != null ? cat.getIcon() : "📌");
                    iconLabel.setStyle("-fx-font-size: 16px;");
                    Label nameLabel = new Label(cat.getName());
                    HBox box = new HBox(10, iconLabel, nameLabel);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });

        categoryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }

                EventCategory cat = findCategoryByName(item);
                if (cat != null) {
                    setText(cat.getIcon() + " " + cat.getName());
                } else {
                    setText(item);
                }
            }
        });
    }

    private EventCategory findCategoryByName(String name) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getName().equals(name)) {
                    return cat;
                }
            }
        }
        return null;
    }

    private EventCategory findCategoryById(int id) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getId() == id) {
                    return cat;
                }
            }
        }
        return null;
    }

    /**
     * Réinitialise tous les flags pristine (utilisé en mode édition)
     */
    private void resetPristineFlags() {
        titlePristine = true;
        descriptionPristine = true;
        startDatePristine = true;
        endDatePristine = true;
        locationPristine = true;
        capacityPristine = true;
        categoryPristine = true;
        pricePristine = true;
    }

    /**
     * Configuration des validations en temps réel
     */
    private void setupValidationListeners() {
        // ==================== TITRE ====================
        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            titlePristine = false;
            validateForm();
        });

        titleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && titleField.getText().trim().isEmpty()) {
                titlePristine = false;
                validateForm();
            }
        });

        // ==================== DESCRIPTION ====================
        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionPristine = false;
            updateDescriptionCounter();
            validateForm();
        });

        descriptionArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && descriptionArea.getText().trim().isEmpty()) {
                descriptionPristine = false;
                validateForm();
            }
        });

        // ==================== DATES ====================
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            startDatePristine = false;
            validateForm();
        });

        startDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && startDatePicker.getValue() == null) {
                startDatePristine = false;
                validateForm();
            }
        });

        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            endDatePristine = false;
            validateForm();
        });

        endDatePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && endDatePicker.getValue() == null) {
                endDatePristine = false;
                validateForm();
            }
        });

        // Spinners heures/minutes
        startHourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            startDatePristine = false;
            validateForm();
        });

        startMinuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            startDatePristine = false;
            validateForm();
        });

        endHourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            endDatePristine = false;
            validateForm();
        });

        endMinuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            endDatePristine = false;
            validateForm();
        });

        // ==================== LIEU (ComboBox) ====================
        villeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            locationPristine = false;
            validateForm();
        });

        locationCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            locationPristine = false;
            validateForm();
        });

        // Gouvernorat et Ville
        gouvernoratCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                locationPristine = false;
                validateForm();
            }
        });

        // ==================== CAPACITÉ ====================
        capacityField.textProperty().addListener((obs, oldVal, newVal) -> {
            capacityPristine = false;
            // Limiter aux chiffres seulement
            if (!newVal.matches("\\d*")) {
                capacityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            validateForm();
        });

        capacityField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && capacityField.getText().trim().isEmpty()) {
                capacityPristine = false;
                validateForm();
            }
        });

        // ==================== CATÉGORIE ====================
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            categoryPristine = false;
            validateForm();
        });

        // ==================== PRIX ====================
        freeCheckbox.selectedProperty().addListener((obs, old, isFree) -> {
            priceField.setDisable(isFree);
            if (isFree) {
                priceField.setText("0");
                priceError.setVisible(false);
                clearFieldStyles(priceField);
            } else {
                pricePristine = false;
            }
            validateForm();
        });

        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            pricePristine = false;
            // Limiter au format décimal
            if (!newVal.matches("\\d*\\.?\\d*")) {
                priceField.setText(oldVal);
            }
            validateForm();
        });

        priceField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !freeCheckbox.isSelected() && priceField.getText().trim().isEmpty()) {
                pricePristine = false;
                validateForm();
            }
        });
    }

    /**
     * Met à jour le compteur de caractères de la description
     */
    private void updateDescriptionCounter() {
        if (descriptionCounter != null) {
            int length = descriptionArea.getText().length();
            descriptionCounter.setText(length + "/" + DESC_MAX_LENGTH);

            // Changer la couleur si dépassement
            if (length > DESC_MAX_LENGTH) {
                descriptionCounter.setStyle("-fx-text-fill: #ef4444;");
            } else if (length >= DESC_MAX_LENGTH - 100) {
                descriptionCounter.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                descriptionCounter.setStyle("-fx-text-fill: #6c757d;");
            }
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

        if (!categoryCombo.getItems().isEmpty()) {
            categoryCombo.setValue(categoryCombo.getItems().get(0));
        }
    }

    /**
     * Configure les listeners pour afficher la météo en temps réel
     */
    private void setupWeatherListeners() {
        // Listener sur la ville (utilisée pour la météo)
        villeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadWeatherPreview();
        });

        // Listener sur la date de début
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadWeatherPreview();
        });
    }

    /**
     * Charge et affiche la météo prévue
     */
    private void loadWeatherPreview() {
        String ville = villeCombo.getValue();
        LocalDate date = startDatePicker.getValue();

        // Vérifier que ville et date sont remplis
        if (ville == null || ville.isEmpty() || date == null) {
            hideWeatherContainer();
            return;
        }

        // Appeler le service météo en arrière-plan
        Thread weatherThread = new Thread(() -> {
            try {
                // Créer un événement temporaire avec la ville pour la météo
                Event tempEvent = new Event();
                tempEvent.setLocation(ville); // Utiliser ville pour Geocoding API
                tempEvent.setStartDate(LocalDateTime.of(date, LocalTime.of(9, 0)));

                System.out.println("🌤️ Chargement météo pour " + ville + " le " + date);
                WeatherData weather = weatherService.getWeatherForEvent(tempEvent);

                // Afficher les résultats sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    if (weather != null && weather.isAvailable()) {
                        displayWeatherPreview(weather);
                    } else {
                        showWeatherError(weather != null ? weather.getErrorMessage() : "Météo indisponible");
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur chargement météo: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    showWeatherError("Erreur lors du chargement de la météo");
                });
            }
        });
        weatherThread.setDaemon(true);
        weatherThread.start();
    }

    /**
     * Affiche les données météorologiques
     */
    private void displayWeatherPreview(WeatherData weather) {
        if (weatherContainer == null) return;

        try {
            // Rendre le container visible
            weatherContainer.setVisible(true);
            weatherContainer.setManaged(true);

            // Remplir les données météo
            formWeatherEmoji.setText(weather.getWeatherEmoji());
            formWeatherTemp.setText(String.format("%.1f°C", weather.getTemperature()));
            formWeatherDescription.setText(weather.getDescription());
            formWeatherRain.setText(weather.getRainChance() + "%");
            formWeatherHumidity.setText(String.format("%.0f%%", weather.getHumidity()));

            // Masquer les erreurs
            formWeatherError.setVisible(false);
            formWeatherError.setManaged(false);

            // Afficher une alerte si conditions défavorables
            if (weather.getRainChance() > 60) {
                formWeatherAlert.setText("⚠️ Risque de pluie élevé! Prévoyez une salle couverte ou un plan B");
                formWeatherAlert.setVisible(true);
                formWeatherAlert.setManaged(true);
            } else {
                formWeatherAlert.setVisible(false);
                formWeatherAlert.setManaged(false);
            }

            System.out.println("✅ Météo affichée: " + weather.getWeatherEmoji() + " " +
                    weather.getTemperature() + "°C");

        } catch (Exception e) {
            System.err.println("❌ Erreur affichage météo: " + e.getMessage());
            showWeatherError("Erreur lors de l'affichage de la météo");
        }
    }

    /**
     * Affiche un message d'erreur
     */
    private void showWeatherError(String errorMsg) {
        if (weatherContainer == null) return;

        weatherContainer.setVisible(true);
        weatherContainer.setManaged(true);

        formWeatherError.setText("❌ " + errorMsg);
        formWeatherError.setVisible(true);
        formWeatherError.setManaged(true);

        formWeatherAlert.setVisible(false);
        formWeatherAlert.setManaged(false);

        System.out.println("⚠️ Erreur météo: " + errorMsg);
    }

    /**
     * Masque le container météo
     */
    private void hideWeatherContainer() {
        if (weatherContainer != null) {
            weatherContainer.setVisible(false);
            weatherContainer.setManaged(false);
        }
    }

    // ==================== VALIDATION ====================

    private boolean validateForm() {
        boolean isValid = true;

        // Réinitialiser tous les erreurs
        resetValidationUI();

        // ==================== 1. VALIDATION DU TITRE ====================
        if (!titlePristine) {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                titleError.setText("❌ Le titre est requis");
                titleError.setVisible(true);
                applyErrorStyle(titleField);
                isValid = false;
            } else if (title.length() < TITLE_MIN_LENGTH) {
                titleError.setText("❌ Min " + TITLE_MIN_LENGTH + " caractères");
                titleError.setVisible(true);
                applyErrorStyle(titleField);
                isValid = false;
            } else if (title.length() > TITLE_MAX_LENGTH) {
                titleError.setText("❌ Max " + TITLE_MAX_LENGTH + " caractères");
                titleError.setVisible(true);
                applyErrorStyle(titleField);
                isValid = false;
            } else {
                applySuccessStyle(titleField);
            }
        }

        // ==================== 2. VALIDATION DE LA DESCRIPTION ====================
        if (!descriptionPristine) {
            String description = descriptionArea.getText().trim();
            if (description.isEmpty()) {
                descriptionError.setText("❌ La description est requise");
                descriptionError.setVisible(true);
                applyErrorStyle(descriptionArea);
                isValid = false;
            } else if (description.length() < DESC_MIN_LENGTH) {
                descriptionError.setText("❌ Min " + DESC_MIN_LENGTH + " caractères");
                descriptionError.setVisible(true);
                applyErrorStyle(descriptionArea);
                isValid = false;
            } else if (description.length() > DESC_MAX_LENGTH) {
                descriptionError.setText("❌ Max " + DESC_MAX_LENGTH + " caractères");
                descriptionError.setVisible(true);
                applyErrorStyle(descriptionArea);
                isValid = false;
            } else {
                applySuccessStyle(descriptionArea);
            }
        }

        // ==================== 3. VALIDATION DES DATES ====================
        if (!startDatePristine || !endDatePristine) {
            if (startDatePicker.getValue() == null) {
                dateError.setText("❌ La date de début est requise");
                dateError.setVisible(true);
                applyErrorStyle(startDatePicker);
                isValid = false;
            } else if (endDatePicker.getValue() == null) {
                dateError.setText("❌ La date de fin est requise");
                dateError.setVisible(true);
                applyErrorStyle(endDatePicker);
                isValid = false;
            } else {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startDateTime = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue())
                );
                LocalDateTime endDateTime = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue())
                );

                // Vérifier que la date de début n'est pas dans le passé (pour les nouveaux événements)
                if (currentEvent == null && startDateTime.isBefore(now)) {
                    dateError.setText("❌ La date ne peut pas être dans le passé");
                    dateError.setVisible(true);
                    applyErrorStyle(startDatePicker);
                    isValid = false;
                }
                // Vérifier que la date de fin est après la date de début
                else if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
                    dateError.setText("❌ La fin doit être après le début");
                    dateError.setVisible(true);
                    applyErrorStyle(endDatePicker);
                    isValid = false;
                } else {
                    applySuccessStyle(startDatePicker);
                    applySuccessStyle(endDatePicker);
                }
            }
        }

        // ==================== 4. VALIDATION DU LIEU ====================
        if (!locationPristine) {
            String location = locationCombo.getValue();
            if (location == null || location.isEmpty() || location.equals("Autre (saisir manuellement)")) {
                location = locationCombo.getEditor().getText().trim();
            }

            if (location.isEmpty()) {
                locationError.setText("❌ Le lieu est requis");
                locationError.setVisible(true);
                applyErrorStyle(locationCombo);
                isValid = false;
            } else if (location.length() < LOCATION_MIN_LENGTH) {
                locationError.setText("❌ Min " + LOCATION_MIN_LENGTH + " caractères");
                locationError.setVisible(true);
                applyErrorStyle(locationCombo);
                isValid = false;
            } else if (location.length() > LOCATION_MAX_LENGTH) {
                locationError.setText("❌ Max " + LOCATION_MAX_LENGTH + " caractères");
                locationError.setVisible(true);
                applyErrorStyle(locationCombo);
                isValid = false;
            } else {
                applySuccessStyle(locationCombo);
            }
        }

        // ==================== 5. VALIDATION DE LA CAPACITÉ ====================
        if (!capacityPristine) {
            String capacity = capacityField.getText().trim();
            if (capacity.isEmpty()) {
                capacityError.setText("❌ La capacité est requise");
                capacityError.setVisible(true);
                applyErrorStyle(capacityField);
                isValid = false;
            } else {
                try {
                    int cap = Integer.parseInt(capacity);
                    if (cap < 1) {
                        capacityError.setText("❌ Minimum 1 place");
                        capacityError.setVisible(true);
                        applyErrorStyle(capacityField);
                        isValid = false;
                    } else {
                        applySuccessStyle(capacityField);
                    }
                } catch (NumberFormatException e) {
                    capacityError.setText("❌ Doit être un nombre entier");
                    capacityError.setVisible(true);
                    applyErrorStyle(capacityField);
                    isValid = false;
                }
            }
        }

        // ==================== 6. VALIDATION DE LA CATÉGORIE ====================
        if (!categoryPristine) {
            if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) {
                categoryError.setText("❌ La catégorie est requise");
                categoryError.setVisible(true);
                applyErrorStyle(categoryCombo);
                isValid = false;
            } else {
                applySuccessStyle(categoryCombo);
            }
        }

        // ==================== 7. VALIDATION DU PRIX ====================
        if (!pricePristine && !freeCheckbox.isSelected()) {
            String price = priceField.getText().trim();
            if (price.isEmpty()) {
                priceError.setText("❌ Le prix est requis si non gratuit");
                priceError.setVisible(true);
                applyErrorStyle(priceField);
                isValid = false;
            } else {
                try {
                    double priceValue = Double.parseDouble(price);
                    if (priceValue < 0) {
                        priceError.setText("❌ Le prix ne peut pas être négatif");
                        priceError.setVisible(true);
                        applyErrorStyle(priceField);
                        isValid = false;
                    } else {
                        applySuccessStyle(priceField);
                    }
                } catch (NumberFormatException e) {
                    priceError.setText("❌ Format invalide (ex: 25.50)");
                    priceError.setVisible(true);
                    applyErrorStyle(priceField);
                    isValid = false;
                }
            }
        }

        // Activer/désactiver le bouton save
        saveBtn.setDisable(!isValid);

        return isValid;
    }

    /**
     * Réinitialise tous les messages d'erreur et styles
     */
    private void resetValidationUI() {
        titleError.setVisible(false);
        descriptionError.setVisible(false);
        dateError.setVisible(false);
        locationError.setVisible(false);
        capacityError.setVisible(false);
        categoryError.setVisible(false);
        priceError.setVisible(false);

        clearFieldStyles(titleField);
        clearFieldStyles(descriptionArea);
        clearFieldStyles(startDatePicker);
        clearFieldStyles(endDatePicker);
        clearFieldStyles(locationCombo);
        clearFieldStyles(capacityField);
        clearFieldStyles(categoryCombo);
        clearFieldStyles(priceField);
    }

    /**
     * Applique le style d'erreur (bordure rouge)
     */
    private void applyErrorStyle(Control field) {
        if (field instanceof TextArea) {
            field.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-background-color: #fff5f5;");
        } else {
            field.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-background-color: #fff5f5;");
        }
    }

    /**
     * Applique le style de succès (bordure verte)
     */
    private void applySuccessStyle(Control field) {
        if (field instanceof TextArea) {
            field.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-background-color: #f0fdf4;");
        } else {
            field.setStyle("-fx-border-color: #10b981; -fx-border-width: 2; -fx-background-color: #f0fdf4;");
        }
    }

    /**
     * Efface les styles personnalisés d'un champ
     */
    private void clearFieldStyles(Control field) {
        field.setStyle("-fx-border-color: #ced4da; -fx-background-color: #f8f9fa;");
    }

    private String safeTrim(TextInputControl field) {
        if (field == null) {
            return "";
        }
        String text = field.getText();
        return text == null ? "" : text.trim();
    }

    // ==================== ACTIONS ====================

    @FXML
    private void handleBack() {
        if (helloController != null) {
            helloController.showEventsList();
        }
    }

    @FXML
    private void handleCancel() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Annuler les modifications?");
        confirmation.setContentText("Les modifications non enregistrées seront perdues.");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                handleBack();
            }
        });
    }

    @FXML
    private void handleSave() {
        // Marquer tous les champs comme non-pristine pour forcer la validation complète
        titlePristine = false;
        descriptionPristine = false;
        startDatePristine = false;
        endDatePristine = false;
        locationPristine = false;
        capacityPristine = false;
        categoryPristine = false;
        pricePristine = false;

        if (!validateForm()) {
            return;
        }

        String title = safeTrim(titleField);
        String description = safeTrim(descriptionArea);
        String gouvernorat = gouvernoratCombo.getValue();
        String ville = villeCombo.getValue();
        String location = locationCombo.getValue();

        // Si l'utilisateur a choisi "Autre (saisir manuellement)" ou qu'il n'y a pas de sélection
        if (location == null || location.isEmpty() || location.equals("Autre (saisir manuellement)")) {
            // Laisser l'utilisateur saisir manuellement
            location = safeTrim(locationCombo.getEditor());
            if (location.isEmpty()) {
                showError("Erreur", "Veuillez sélectionner ou saisir un lieu");
                return;
            }
        }

        String capacityText = safeTrim(capacityField);
        if (capacityText.isEmpty()) {
            showError("Erreur", "Veuillez saisir une capacité valide");
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capacityText);
        } catch (NumberFormatException e) {
            showError("Erreur", "La capacité doit être un nombre entier");
            return;
        }

        String imageUrl = safeTrim(imageUrlField);

        String categoryName = categoryCombo.getValue();
        int categoryId = 1;
        EventCategory selectedCat = findCategoryByName(categoryName);
        if (selectedCat != null) {
            categoryId = selectedCat.getId();
        }

        boolean isFree = freeCheckbox.isSelected();
        double price = 0;
        if (!isFree) {
            String priceText = safeTrim(priceField);
            if (priceText.isEmpty()) {
                showError("Erreur", "Veuillez saisir un prix valide");
                return;
            }
            try {
                price = Double.parseDouble(priceText);
            } catch (NumberFormatException e) {
                showError("Erreur", "Le prix doit être un nombre valide");
                return;
            }
        }

        int creatorId = UserSession.getInstance().getUserId();
        if (creatorId <= 0) {
            showError("Erreur", "Utilisateur non connecté. Veuillez vous reconnecter.");
            return;
        }

        LocalDateTime startDate = LocalDateTime.of(
                startDatePicker.getValue(),
                LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue())
        );

        LocalDateTime endDate = LocalDateTime.of(
                endDatePicker.getValue(),
                LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue())
        );

        try {
            boolean success;

            if (currentEvent == null) {
                // Création
                Event newEvent = new Event(title, description, startDate, endDate,
                        location, capacity, imageUrl, categoryId, creatorId,
                        isFree, price);
                newEvent.setGouvernorat(gouvernorat);
                newEvent.setVille(ville);
                success = eventService.addEvent(newEvent);

                if (success) {
                    showSuccess("Succès", "Événement créé avec succès!");

                    // Rafraîchir les KPI après création
                    if (helloController != null) {
                        helloController.refreshKPIs();
                    }

                    handleBack();
                } else {
                    showError("Erreur", "Impossible de créer l'événement");
                }

            } else {
                // Modification
                currentEvent.setTitle(title);
                currentEvent.setDescription(description);
                currentEvent.setStartDate(startDate);
                currentEvent.setEndDate(endDate);
                currentEvent.setLocation(location);
                currentEvent.setGouvernorat(gouvernorat);
                currentEvent.setVille(ville);
                currentEvent.setCapacity(capacity);
                currentEvent.setImageUrl(imageUrl);
                currentEvent.setCategoryId(categoryId);
                currentEvent.setCreatedBy(creatorId);
                currentEvent.setFree(isFree);
                currentEvent.setTicketPrice(price);

                success = eventService.updateEvent(currentEvent);

                if (success) {
                    showSuccess("Succès", "Événement mis à jour avec succès!");

                    // Rafraîchir les KPI après modification
                    if (helloController != null) {
                        helloController.refreshKPIs();
                    }

                    handleBack();
                } else {
                    showError("Erreur", "Impossible de mettre à jour l'événement");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur", "Une erreur est survenue: " + e.getMessage());
        }
    }

    // ==================== DIALOGS ====================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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

    // ==================== DONNÉES TUNISIENNES ====================

    private Map<String, List<String>> villesParGouvernorat;
    private Map<String, List<String>> facultesParVille;

    /**
     * Charge les données tunisiennes (gouvernorats, villes, facultés)
     */
    private void loadTunisianData() {
        villesParGouvernorat = new LinkedHashMap<>();
        facultesParVille = new LinkedHashMap<>();

        // 24 Gouvernorats tunisiens
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

        // Facultés par ville
        facultesParVille.put("Tunis", Arrays.asList("FST", "FSEG", "FD", "FLSH", "ENIT", "ESSEC", "IHEC"));
        facultesParVille.put("Sousse", Arrays.asList("FSM", "ESSTHS", "ISITCom", "ISSAT"));
        facultesParVille.put("Sfax", Arrays.asList("FSS", "ENIS", "ISIMS"));
        facultesParVille.put("Nabeul", Arrays.asList("ISSAT", "ISET"));
        facultesParVille.put("Monastir", Arrays.asList("FM", "FSM", "ISIM"));

        System.out.println("✅ Données tunisiennes chargées");
    }

    /**
     * Charge les gouvernorats dans le ComboBox
     */
    private void loadGouvernorats() {
        gouvernoratCombo.getItems().clear();
        gouvernoratCombo.getItems().addAll(villesParGouvernorat.keySet());
    }

    /**
     * Configure les listeners pour le filtrage automatique
     */
    private void setupLocationListeners() {
        gouvernoratCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadVillesForGouvernorat(newVal);
                villeCombo.setValue(null);
                locationCombo.setValue(null);
                locationCombo.getItems().clear();
            }
        });

        villeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadFacultesForVille(newVal);
                locationCombo.setValue(null);
            }
        });
    }

    /**
     * Charge les villes pour un gouvernorat donné
     */
    private void loadVillesForGouvernorat(String gouvernorat) {
        villeCombo.getItems().clear();
        List<String> villes = villesParGouvernorat.get(gouvernorat);
        if (villes != null) {
            villeCombo.getItems().addAll(villes);
        }
    }

    /**
     * Charge les facultés pour une ville donnée
     */
    private void loadFacultesForVille(String ville) {
        locationCombo.getItems().clear();
        List<String> facultes = facultesParVille.get(ville);
        if (facultes != null && !facultes.isEmpty()) {
            locationCombo.getItems().addAll(facultes);
        } else {
            locationCombo.getItems().addAll("Autre (saisir manuellement)");
        }
    }
}
