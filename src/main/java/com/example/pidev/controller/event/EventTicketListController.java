package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventTicketService;
import com.example.pidev.service.user.UserService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller pour la liste des tickets
 * @author Ons Abdesslem
 */
public class EventTicketListController {

    // ========== TABLEAU ==========
    @FXML private TableView<EventTicket> ticketTable;
    @FXML private TableColumn<EventTicket, String> ticketCodeCol;
    @FXML private TableColumn<EventTicket, String> eventCol;
    @FXML private TableColumn<EventTicket, String> userCol;
    @FXML private TableColumn<EventTicket, String> statusCol;
    @FXML private TableColumn<EventTicket, String> createdAtCol;
    @FXML private TableColumn<EventTicket, Void> actionsCol;

    // ========== FILTRES ==========
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventFilter;
    @FXML private ComboBox<String> statusFilter;

    // ========== KPI ==========
    @FXML private Label totalLabel;
    @FXML private Label usedLabel;
    @FXML private Label resultLabel;

    // ========== NAVBAR ==========
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    // ========== BOUTONS ==========
    @FXML private Button scanBtn;

    // ========== PAGINATION ==========
    @FXML private HBox paginationContainer;
    @FXML private Button prevBtn;
    @FXML private Label pageInfoLabel;

    private EventTicketService ticketService;
    private EventService eventService;
    private UserService userService;
    private MainController helloController;
    private List<EventTicket> allTickets;
    private List<EventTicket> filteredTickets;
    private List<Event> allEvents;

    // Variables de pagination
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;


    @FXML
    public void initialize() {
        System.out.println("✅ EventTicketListController initialisé");

        ticketService = new EventTicketService();
        eventService = new EventService();
        userService = new UserService();

        setupFilters();
        setupTableColumns();
        loadTickets();

        // ========== DATE/HEURE TEMPS RÉEL ==========
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
        if (timeLabel != null) {
            timeLabel.setText(now.format(timeFormatter));
        }
    }

    public void setMainController(MainController helloController) {
        this.helloController = helloController;
    }

    private void setupFilters() {
        searchField.setPromptText("🔍 Rechercher un ticket par code...");
        searchField.textProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });

        eventFilter.getItems().add("Tous les événements");
        eventFilter.setValue("Tous les événements");
        eventFilter.valueProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });

        statusFilter.getItems().addAll("Tous", "Utilisé", "Non utilisé");
        statusFilter.setValue("Tous");
        statusFilter.valueProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });
    }

    private void setupTableColumns() {
        // Code du ticket
        ticketCodeCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getTicketCode())
        );

        // Événement - Affichage du nom complet
        eventCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(getEventName(param.getValue().getEventId()))
        );
        eventCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String eventName, boolean empty) {
                super.updateItem(eventName, empty);
                if (empty || eventName == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label nameLabel = new Label(eventName);
                    nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b; -fx-font-weight: 500;");
                    setGraphic(nameLabel);
                }
            }
        });

        // Utilisateur (Participant) - Affichage du nom complet
        userCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(getUserFullName(param.getValue().getUserId()))
        );
        userCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String userName, boolean empty) {
                super.updateItem(userName, empty);
                if (empty || userName == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label nameLabel = new Label(userName);
                    nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b; -fx-font-weight: 500;");
                    setGraphic(nameLabel);
                }
            }
        });

        // Statut
        statusCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().isUsed() ? "Utilisé" : "Non utilisé"
                )
        );
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("status-badge");
                    if ("Utilisé".equals(status)) {
                        badge.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                    } else {
                        badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; " +
                                "-fx-padding: 4 12; -fx-background-radius: 20;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Date création
        createdAtCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getFormattedCreatedAt())
        );

        // Actions
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = createIconButton("eye", "#17a2b8");
            private final Button deleteBtn = createIconButton("trash", "#dc3545");
            private final HBox container = new HBox(8, viewBtn, deleteBtn);
            {
                container.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EventTicket ticket = getTableView().getItems().get(getIndex());
                    viewBtn.setOnAction(e -> handleView(ticket));
                    deleteBtn.setOnAction(e -> handleDelete(ticket));
                    setGraphic(container);
                }
            }
        });
    }

    private Button createIconButton(String iconType, String color) {
        Button btn = new Button();
        btn.setMinSize(38, 38);
        btn.setMaxSize(38, 38);
        btn.getStyleClass().add("action-btn-icon");
        SVGPath icon = new SVGPath();
        switch (iconType) {
            case "eye":
                icon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
                break;
            case "qrcode":
                icon.setContent("M4 4h6v6H4V4zm10 0h6v6h-6V4zM4 14h6v6H4v-6zm14 0h-4v2h2v4h2v-4h2v-6h-2v4z");
                break;
            case "trash":
                icon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                break;
        }
        icon.setFill(Color.WHITE);
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
        return btn;
    }

    private void loadTickets() {
        try {
            allTickets = ticketService.getAllTickets();
            populateEventFilter();
            applyFilters();
            updateStatistics();
            System.out.println("✅ " + allTickets.size() + " tickets chargés");
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            allTickets = List.of(); // Liste vide en cas d'erreur
        }
    }

    /**
     * Récupère le nom complet de l'utilisateur depuis la base de données
     * @param userId ID de l'utilisateur
     * @return Nom complet (Prénom Nom) ou "Utilisateur inconnu"
     */
    private String getUserFullName(int userId) {
        try {
            UserModel user = userService.getUserById(userId);
            if (user != null) {
                String firstName = user.getFirst_Name() != null ? user.getFirst_Name() : "";
                String lastName = user.getLast_Name() != null ? user.getLast_Name() : "";
                String fullName = (firstName + " " + lastName).trim();
                return fullName.isEmpty() ? "Utilisateur #" + userId : fullName;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération utilisateur " + userId + ": " + e.getMessage());
        }
        return "Utilisateur #" + userId;
    }

    /**
     * Récupère le nom de l'événement depuis la base de données
     * @param eventId ID de l'événement
     * @return Nom de l'événement ou "Événement #[ID]"
     */
    private String getEventName(int eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            if (event != null && event.getTitle() != null && !event.getTitle().isEmpty()) {
                return event.getTitle();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération événement " + eventId + ": " + e.getMessage());
        }
        return "Événement #" + eventId;
    }

    private void populateEventFilter() {
        eventFilter.getItems().clear();
        eventFilter.getItems().add("Tous les événements");

        // Charger tous les événements depuis la BDD
        try {
            allEvents = eventService.getAllEvents();

            // Ajouter les événements qui ont au moins un ticket
            List<Integer> eventIdsWithTickets = allTickets.stream()
                    .map(EventTicket::getEventId)
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

            for (Integer eventId : eventIdsWithTickets) {
                Event event = eventService.getEventById(eventId);
                if (event != null && event.getTitle() != null) {
                    eventFilter.getItems().add(event.getTitle());
                }
            }

            System.out.println("✅ Filtre événements mis à jour avec " + eventIdsWithTickets.size() + " événements");
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement événements pour filtre: " + e.getMessage());
        }

        eventFilter.setValue("Tous les événements");
    }

    private void updateStatistics() {
        if (allTickets == null) return;
        int total = allTickets.size();
        long used = allTickets.stream().filter(EventTicket::isUsed).count();
        totalLabel.setText(String.valueOf(total));
        usedLabel.setText(String.valueOf(used));
    }

    private void applyFilters() {
        if (allTickets == null) return;

        String searchText = searchField.getText().toLowerCase().trim();
        String eventName = eventFilter.getValue();
        String status = statusFilter.getValue();

        List<EventTicket> filtered = allTickets.stream()
                .filter(ticket -> {
                    boolean matchSearch = searchText.isEmpty() ||
                            ticket.getTicketCode().toLowerCase().contains(searchText);

                    // Filtre par nom d'événement (dynamique)
                    boolean matchEvent = eventName == null || "Tous les événements".equals(eventName);
                    if (!matchEvent) {
                        String ticketEventName = getEventName(ticket.getEventId());
                        matchEvent = ticketEventName.equals(eventName);
                    }

                    boolean matchStatus = status == null || "Tous".equals(status) ||
                            (status.equals("Utilisé") && ticket.isUsed()) ||
                            (status.equals("Non utilisé") && !ticket.isUsed());

                    return matchSearch && matchEvent && matchStatus;
                })
                .toList();

        filteredTickets = filtered;
        currentPage = 1;
        resultLabel.setText(filtered.size() + " résultat(s) trouvé(s)");
        setupPagination();
    }


    @FXML
    private void handleScan() {
        // Ouvrir la vue de scan QR code
        System.out.println("📱 Ouverture du scanner QR code");
        // TODO: Implémenter le scan
    }

    private void handleView(EventTicket ticket) {
        if (helloController != null) {
            helloController.showTicketView(ticket);
        } else {
            System.err.println("❌ MainController est null !");
        }
    }

    private void handleScan(EventTicket ticket) {
        // Marquer comme utilisé
        if (!ticket.isUsed()) {
            boolean success = ticketService.markTicketAsUsed(ticket.getId());
            if (success) {
                showSuccess("Succès", "Ticket scanné et marqué comme utilisé");
                loadTickets();
            } else {
                showError("Erreur", "Impossible de scanner le ticket");
            }
        } else {
            showError("Erreur", "Ce ticket a déjà été utilisé");
        }
    }

    private void handleDelete(EventTicket ticket) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le ticket " + ticket.getTicketCode() + " ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (ticketService.deleteTicket(ticket.getId())) {
                    showSuccess("Succès", "Ticket supprimé");
                    loadTickets();
                } else {
                    showError("Erreur", "Impossible de supprimer");
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadTickets();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== PAGINATION =====================

    private void setupPagination() {
        if (filteredTickets == null || filteredTickets.isEmpty()) {
            totalPages = 1;
            currentPage = 1;
            updatePaginationUI();
            ticketTable.getItems().clear();
            return;
        }

        totalPages = (int) Math.ceil((double) filteredTickets.size() / itemsPerPage);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        updatePaginationUI();
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        if (filteredTickets == null || filteredTickets.isEmpty()) {
            ticketTable.getItems().clear();
            return;
        }

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredTickets.size());

        List<EventTicket> pageItems = filteredTickets.subList(fromIndex, toIndex);
        ticketTable.getItems().setAll(pageItems);
    }

    private void updatePaginationUI() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + currentPage + " sur " + totalPages);
        }

        if (paginationContainer == null) return;

        paginationContainer.getChildren().clear();

        // Bouton Précédent
        if (prevBtn != null) {
            prevBtn.setDisable(currentPage == 1);
        }

        // Ajouter les boutons de pages
        int maxButtons = 5;
        int startPage, endPage;

        if (totalPages <= maxButtons) {
            startPage = 1;
            endPage = totalPages;
        } else {
            int halfButtons = maxButtons / 2;
            if (currentPage <= halfButtons + 1) {
                startPage = 1;
                endPage = maxButtons;
            } else if (currentPage >= totalPages - halfButtons) {
                startPage = totalPages - maxButtons + 1;
                endPage = totalPages;
            } else {
                startPage = currentPage - halfButtons;
                endPage = currentPage + halfButtons;
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            final int pageNum = i;
            Button pageBtn = new Button(String.valueOf(i));
            if (i == currentPage) {
                pageBtn.getStyleClass().add("btn-page-active");
                pageBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
            } else {
                pageBtn.getStyleClass().add("btn-page");
                pageBtn.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
            }
            pageBtn.setOnAction(event -> goToPage(pageNum));
            paginationContainer.getChildren().add(pageBtn);
        }

        if (endPage < totalPages) {
            Label dots = new Label("...");
            dots.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 10; -fx-font-size: 14px;");
            paginationContainer.getChildren().add(dots);

            Button lastPageBtn = new Button(String.valueOf(totalPages));
            lastPageBtn.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #e2e8f0; " +
                    "-fx-border-width: 1; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8; -fx-cursor: hand;");
            lastPageBtn.setOnAction(event -> goToPage(totalPages));
            paginationContainer.getChildren().add(lastPageBtn);
        }

        // Ajouter le bouton Suivant (UN SEUL)
        Button nextBtn = new Button("Suivant »");
        nextBtn.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-padding: 8 16; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        nextBtn.setDisable(currentPage == totalPages);
        nextBtn.setOnAction(event -> handleNextPage());
        paginationContainer.getChildren().add(nextBtn);
    }

    private void goToPage(int pageNum) {
        if (pageNum >= 1 && pageNum <= totalPages && pageNum != currentPage) {
            currentPage = pageNum;
            displayCurrentPage();
            updatePaginationUI();
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            displayCurrentPage();
            updatePaginationUI();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            displayCurrentPage();
            updatePaginationUI();
        }
    }
}