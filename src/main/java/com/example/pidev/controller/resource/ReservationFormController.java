package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.resource.*;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.resource.*;
import com.example.pidev.utils.UserSession;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationFormController {

    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<Object> itemCombo;
    @FXML private ComboBox<Event> eventCombo;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private TextField quantityField;
    @FXML private ImageView imagePreview;
    @FXML private Button btnValider;
    @FXML private Label userInfoLabel;

    @FXML private Button voiceBtn;
    @FXML private Label voiceStatusLabel;

    private final ReservationService resService = new ReservationService();
    private final SalleService salleService = new SalleService();
    private final EquipementService eqService = new EquipementService();
    private final EventService eventService = new EventService();

    private ReservationResource selectedReservation = null;
    private int currentUserId;
    private String currentUserName;

    private VoiceRecognitionService voiceService;
    private boolean isListening = false;

    @FXML
    public void initialize() {
        UserSession session = UserSession.getInstance();
        currentUserId = session.getUserId();
        currentUserName = session.getFullName();
        configurerCalendrier();

        if (userInfoLabel != null) {
            if (session.isLoggedIn()) {
                userInfoLabel.setText("Réservation pour: " + currentUserName + " (ID: " + currentUserId + ")");
                userInfoLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
            } else {
                userInfoLabel.setText("⚠️ Aucun utilisateur connecté");
                userInfoLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        }

        System.out.println("🔵 ReservationFormController - Utilisateur connecté: " + currentUserName + " (ID: " + currentUserId + ")");

        typeCombo.setItems(FXCollections.observableArrayList("SALLE", "EQUIPEMENT"));
        typeCombo.setOnAction(e -> chargerRessources());
        itemCombo.setOnAction(e -> mettreAJourApercu());
        startDatePicker.valueProperty().addListener((obs, oldV, newV) -> refreshItemCombo());
        endDatePicker.valueProperty().addListener((obs, oldV, newV) -> refreshItemCombo());
        setupItemComboDesign();

        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                quantityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // --- CHARGEMENT DES ÉVÉNEMENTS ---
        List<Event> events = eventService.getAllEvents();
        eventCombo.setItems(FXCollections.observableArrayList(events));
        eventCombo.setPromptText("-- Aucun événement lié (optionnel) --");

        eventCombo.setCellFactory(lv -> new ListCell<Event>() {
            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getTitle() + " (" + item.getFormattedStartDate() + ")");
            }
        });
        eventCombo.setButtonCell(new ListCell<Event>() {
            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null)
                        ? "-- Aucun événement lié (optionnel) --"
                        : item.getTitle() + " (" + item.getFormattedStartDate() + ")");
            }
        });
    }

    // --- VOCAL ---

    @FXML
    private void toggleVoiceControl() {
        if (!isListening) {
            startVoiceControl();
        } else {
            stopVoiceControl();
        }
    }

    private void startVoiceControl() {
        isListening = true;
        if (voiceBtn != null) voiceBtn.setStyle("-fx-background-radius: 50; -fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 18;");
        if (voiceStatusLabel != null) voiceStatusLabel.setText("🎙️ Écoute activée...");

        voiceService = new VoiceRecognitionService((String jsonResult) -> {
            try {
                JSONObject json = new JSONObject(jsonResult);
                String text = json.optString("text", "").toLowerCase();
                if (!text.isEmpty()) {
                    Platform.runLater(() -> {
                        if (voiceStatusLabel != null) voiceStatusLabel.setText("Compris : " + text);
                        handleVoiceCommand(text);
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur parsing vocal: " + e.getMessage());
            }
        });

        if (voiceService != null) {
            voiceService.setDaemon(true);
            voiceService.start();
        }
    }

    private void stopVoiceControl() {
        isListening = false;
        if (voiceService != null) {
            voiceService.stopListening();
        }
        if (voiceBtn != null) voiceBtn.setStyle("-fx-background-radius: 50; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 18;");
        if (voiceStatusLabel != null) voiceStatusLabel.setText("Micro désactivé");
    }

    private void handleVoiceCommand(String command) {
        System.out.println("🎙️ Analyse de l'ordre : " + command);
        String lowerCmd = command.toLowerCase();

        if (lowerCmd.contains("annule") || lowerCmd.contains("quitter") || lowerCmd.contains("retour")) {
            System.out.println("🚫 Action vocale : Annulation");
            goBack();
            return;
        }

        if (lowerCmd.contains("réserve") || lowerCmd.contains("valide") || lowerCmd.contains("confirme")) {
            System.out.println("✅ Action vocale : Validation");
            validerAction();
            return;
        }

        if (lowerCmd.contains("salle")) {
            typeCombo.setValue("SALLE");
            chargerRessources();
        } else if (lowerCmd.contains("équipement") || lowerCmd.contains("matériel")) {
            typeCombo.setValue("EQUIPEMENT");
            chargerRessources();
        }

        String numeric = lowerCmd.replaceAll("[^0-9]", "");
        if (!numeric.isEmpty()) {
            quantityField.setText(numeric);
        } else {
            if (lowerCmd.contains("dix")) quantityField.setText("10");
            else if (lowerCmd.contains("quinze")) quantityField.setText("15");
            else if (lowerCmd.contains("vingt")) quantityField.setText("20");
            else if (lowerCmd.contains("trente")) quantityField.setText("30");
            else if (lowerCmd.contains("quarante")) quantityField.setText("40");
            else if (lowerCmd.contains("cinquante")) quantityField.setText("50");
            else if (lowerCmd.contains("un")) quantityField.setText("1");
            else if (lowerCmd.contains("deux")) quantityField.setText("2");
            else if (lowerCmd.contains("trois")) quantityField.setText("3");
        }

        if (lowerCmd.contains("début") || lowerCmd.contains("commence")) {
            LocalDate date = parseVoiceDate(lowerCmd);
            if (date != null) startDatePicker.setValue(date);
        } else if (lowerCmd.contains("fin") || lowerCmd.contains("termine")) {
            LocalDate date = parseVoiceDate(lowerCmd);
            if (date != null) endDatePicker.setValue(date);
        }

        itemCombo.getItems().stream()
                .filter(item -> lowerCmd.contains(item.toString().toLowerCase()))
                .findFirst()
                .ifPresent(item -> {
                    itemCombo.setValue(item);
                    mettreAJourApercu();
                });

        // Recherche vocale dans les événements
        eventCombo.getItems().stream()
                .filter(ev -> lowerCmd.contains(ev.getTitle().toLowerCase()))
                .findFirst()
                .ifPresent(eventCombo::setValue);
    }

    private LocalDate parseVoiceDate(String text) {
        try {
            int day = 1;
            String numeric = text.replaceAll("[^0-9]", "");
            if (!numeric.isEmpty()) day = Integer.parseInt(numeric);
            else if (text.contains("premier")) day = 1;

            int month = LocalDate.now().getMonthValue();
            if (text.contains("janvier")) month = 1;
            else if (text.contains("février")) month = 2;
            else if (text.contains("mars")) month = 3;
            else if (text.contains("avril")) month = 4;
            else if (text.contains("mai")) month = 5;
            else if (text.contains("juin")) month = 6;
            else if (text.contains("juillet")) month = 7;
            else if (text.contains("août")) month = 8;
            else if (text.contains("septembre")) month = 9;
            else if (text.contains("octobre")) month = 10;
            else if (text.contains("novembre")) month = 11;
            else if (text.contains("décembre")) month = 12;

            return LocalDate.of(2026, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    // --- MÉTHODES ORIGINALES ---

    public void setReservationToEdit(ReservationResource res) {
        this.selectedReservation = res;
        if (res != null) {
            typeCombo.setValue(res.getResourceType());
            chargerRessources();
            startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                itemCombo.setCellFactory(null);
                setupItemComboDesign();
            });
            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                itemCombo.setCellFactory(null);
                setupItemComboDesign();
            });
            quantityField.setText(String.valueOf(res.getQuantity()));
            btnValider.setText("Mettre à jour");

            for (Object obj : itemCombo.getItems()) {
                if (obj instanceof Salle s && s.getId() == res.getSalleId()) {
                    itemCombo.setValue(obj);
                    break;
                }
                if (obj instanceof Equipement eq && eq.getId() == res.getEquipementId()) {
                    itemCombo.setValue(obj);
                    break;
                }
            }

            // Pré-sélectionner l'événement lié
            if (res.getEventId() != null) {
                eventCombo.getItems().stream()
                        .filter(ev -> ev.getId() == res.getEventId())
                        .findFirst()
                        .ifPresent(eventCombo::setValue);
            }

            if (userInfoLabel != null) {
                userInfoLabel.setText("Modification réservation #" + res.getId() + " - " + currentUserName);
            }
        }
    }

    @FXML
    void validerAction() {
        try {
            if (currentUserId == -1) {
                new Alert(Alert.AlertType.ERROR, "❌ Vous devez être connecté pour effectuer une réservation").show();
                return;
            }

            if (itemCombo.getValue() == null || startDatePicker.getValue() == null) {
                new Alert(Alert.AlertType.ERROR, "Veuillez remplir tous les champs obligatoires").show();
                return;
            }

            LocalDateTime s = startDatePicker.getValue().atTime(8, 0);
            LocalDateTime e = endDatePicker.getValue() == null ? s.plusHours(2) : endDatePicker.getValue().atTime(18, 0);

            Object sel = itemCombo.getValue();

            if (quantityField.getText().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Veuillez saisir une quantité").show();
                return;
            }

            int qtySaisie = Integer.parseInt(quantityField.getText());
            if (qtySaisie <= 0) {
                new Alert(Alert.AlertType.ERROR, "La quantité doit être supérieure à 0").show();
                return;
            }

            int currentId = (selectedReservation == null) ? -1 : selectedReservation.getId();

            int dispo = 0;
            String nomRessource = "";

            if (sel instanceof Salle sa) {
                dispo = resService.isSalleOccupee(sa.getId(), s, e, currentId) ? 0 : 1;
                nomRessource = sa.getName();
            } else if (sel instanceof Equipement eq) {
                dispo = resService.getStockTotalEquipement(eq.getId()) - resService.getStockOccupe(eq.getId(), s, e, currentId);
                nomRessource = eq.getName();
            }

            if (qtySaisie > dispo) {
                new Alert(Alert.AlertType.ERROR, "Stock insuffisant : " + dispo + " disponible(s)").show();
                return;
            }

            ReservationResource res = new ReservationResource(
                    currentId == -1 ? 0 : currentId,
                    typeCombo.getValue(),
                    (sel instanceof Salle sa) ? sa.getId() : null,
                    (sel instanceof Equipement eq) ? eq.getId() : null,
                    s, e, qtySaisie
            );

            res.setUserId(currentUserId);

            // Lier l'événement sélectionné
            Event selectedEvent = eventCombo.getValue();
            if (selectedEvent != null) {
                res.setEventId(selectedEvent.getId());
            }

            String message;
            if (selectedReservation == null) {
                resService.ajouter(res);
                message = "✅ Réservation créée avec succès pour " + currentUserName;
                System.out.println("📧 Tentative d'envoi d'email pour : " + nomRessource);
                envoyerEmailConfirmation(res, nomRessource);
            } else {
                resService.modifier(res);
                message = "✅ Réservation modifiée avec succès";
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION, message);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.showAndWait();

            goBack();

        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "La quantité doit être un nombre valide").show();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).show();
        }
    }

    private void chargerRessources() {
        itemCombo.getItems().clear();
        if ("SALLE".equals(typeCombo.getValue())) {
            itemCombo.setItems(FXCollections.observableArrayList(salleService.afficher()));
        } else if ("EQUIPEMENT".equals(typeCombo.getValue())) {
            itemCombo.setItems(FXCollections.observableArrayList(eqService.afficher()));
        }
    }

    private void mettreAJourApercu() {
        Object sel = itemCombo.getValue();
        if (sel != null) {
            String p = (sel instanceof Salle s) ? s.getImagePath() : (sel instanceof Equipement eq ? eq.getImagePath() : null);
            if (p != null) {
                try {
                    imagePreview.setImage(new Image(p));
                } catch (Exception ex) {
                    System.err.println("Erreur image: " + p);
                }
            }
        }
    }

    private void setupItemComboDesign() {
        itemCombo.setCellFactory(lv -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    LocalDateTime s = (startDatePicker.getValue() != null) ? startDatePicker.getValue().atTime(8, 0) : LocalDateTime.now();
                    LocalDateTime e = (endDatePicker.getValue() != null) ? endDatePicker.getValue().atTime(18, 0) : s.plusHours(2);

                    String name = "";
                    int dispo = 0;
                    int currentId = (selectedReservation == null) ? -1 : selectedReservation.getId();

                    if (item instanceof Salle sa) {
                        name = sa.getName();
                        dispo = resService.isSalleOccupee(sa.getId(), s, e, currentId) ? 0 : 1;
                    } else if (item instanceof Equipement eq) {
                        name = eq.getName();
                        dispo = resService.getStockTotalEquipement(eq.getId()) - resService.getStockOccupe(eq.getId(), s, e, currentId);
                    }

                    if (dispo > 0) {
                        setText(name + " (Disponible: " + dispo + ")");
                        setTextFill(Color.BLACK);
                        setDisable(false);
                        setStyle("-fx-opacity: 1.0;");
                    } else {
                        setText(name + " (Indisponible)");
                        setTextFill(Color.RED);
                        setDisable(true);
                        setStyle("-fx-opacity: 0.5;");
                    }
                }
            }
        });
        itemCombo.setButtonCell((ListCell) itemCombo.getCellFactory().call(null));
    }

    private void refreshItemCombo() {
        itemCombo.setCellFactory(null);
        setupItemComboDesign();
    }

    @FXML
    void goBack() {
        stopVoiceControl();
        MainController.getInstance().showReservations();
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        if (userInfoLabel != null) {
            userInfoLabel.setText("Réservation pour utilisateur ID: " + userId);
        }
    }

    private void envoyerEmailConfirmation(ReservationResource res, String nomRessource) {
        final String username = "mecherguisouhail8@gmail.com";
        final String password = "xknx cbvx piuc upbb";

        java.util.Properties prop = new java.util.Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        javax.mail.Session session = javax.mail.Session.getInstance(prop, new javax.mail.Authenticator() {
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(username, password);
            }
        });

        new Thread(() -> {
            try {
                javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
                message.setFrom(new javax.mail.internet.InternetAddress(username));
                String emailDestinataire = "mecherguisouhail8@gmail.com";
                message.setRecipients(javax.mail.Message.RecipientType.TO,
                        javax.mail.internet.InternetAddress.parse(emailDestinataire));
                message.setSubject("📌 Confirmation Réservation : " + nomRessource);

                // Ajout de l'événement dans l'email si présent
                String eventInfo = "";
                if (res.getEventId() != null) {
                    Event ev = eventService.getEventById(res.getEventId());
                    if (ev != null) {
                        eventInfo = "<p>Événement lié : <strong>" + ev.getTitle() + "</strong> (" + ev.getFormattedStartDate() + ")</p>";
                    }
                }

                String corpsMail = "<h2>Réservation Confirmée !</h2>"
                        + "<p>Bonjour " + currentUserName + ",</p>"
                        + "<p>Votre réservation pour <strong>" + nomRessource + "</strong> a été enregistrée.</p>"
                        + "<p>Quantité : " + res.getQuantity() + "</p>"
                        + eventInfo;

                message.setContent(corpsMail, "text/html; charset=utf-8");
                javax.mail.Transport.send(message);
                System.out.println("✅ Email envoyé avec succès de Gmail !");

            } catch (Exception e) {
                System.err.println("❌ Erreur Mail : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void configurerCalendrier() {
        javafx.util.Callback<DatePicker, DateCell> dayCellFactory = dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #eeeeee;");
                }
            }
        };
        startDatePicker.setDayCellFactory(dayCellFactory);
        endDatePicker.setDayCellFactory(dayCellFactory);
    }
}