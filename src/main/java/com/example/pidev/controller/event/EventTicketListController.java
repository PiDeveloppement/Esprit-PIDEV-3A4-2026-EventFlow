package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventTicketService;
import com.example.pidev.service.user.UserService;
import com.example.pidev.utils.UserSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EventTicketListController {

    @FXML private TableView<EventTicket> ticketTable;
    @FXML private TableColumn<EventTicket, String> ticketCodeCol;
    @FXML private TableColumn<EventTicket, String> eventCol;
    @FXML private TableColumn<EventTicket, String> userCol;
    @FXML private TableColumn<EventTicket, String> statusCol;
    @FXML private TableColumn<EventTicket, String> createdAtCol;
    @FXML private TableColumn<EventTicket, Void> actionsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private Label totalLabel;
    @FXML private Label usedLabel;
    @FXML private Label resultLabel;

    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    @FXML private HBox paginationContainer;
    @FXML private Button prevBtn;
    @FXML private Label pageInfoLabel;

    private EventTicketService ticketService;
    private EventService eventService;
    private UserService userService;
    private MainController mainController;
    private List<EventTicket> allTickets = List.of();
    private List<EventTicket> filteredTickets = List.of();
    private List<Event> allEvents = List.of();

    private int currentPage = 1;
    private final int itemsPerPage = 10;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        ticketService = new EventTicketService();
        eventService = new EventService();
        userService = new UserService();

        setupFilters();
        setupTableColumns();
        loadTickets();

        updateDateTime();
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateDateTime()),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
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

    private void setupFilters() {
        if (searchField != null) {
            searchField.setPromptText("Recherche ticket...");
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = 1;
                applyFilters();
            });
        }

        if (eventFilter != null) {
            eventFilter.getItems().setAll("Tous les evenements");
            eventFilter.setValue("Tous les evenements");
            eventFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = 1;
                applyFilters();
            });
        }

        if (statusFilter != null) {
            statusFilter.getItems().setAll("Tous", "Utilise", "Non utilise");
            statusFilter.setValue("Tous");
            statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentPage = 1;
                applyFilters();
            });
        }
    }

    private void setupTableColumns() {
        ticketCodeCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getTicketCode())
        );

        eventCol.setCellValueFactory(param ->
                new SimpleStringProperty(getEventName(param.getValue().getEventId()))
        );

        userCol.setCellValueFactory(param ->
                new SimpleStringProperty(getUserFullName(param.getValue().getUserId()))
        );

        statusCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().isUsed() ? "Utilise" : "Non utilise")
        );

        createdAtCol.setCellValueFactory(param ->
                new SimpleStringProperty(param.getValue().getFormattedCreatedAt())
        );

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = createIconButton("eye", "#17a2b8");
            private final Button scanBtn = createIconButton("qrcode", "#0d47a1");
            private final Button deleteBtn = createIconButton("trash", "#dc3545");
            private final HBox box = new HBox(8, viewBtn, scanBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                EventTicket ticket = getTableView().getItems().get(getIndex());
                viewBtn.setOnAction(e -> handleView(ticket));
                scanBtn.setOnAction(e -> handleScan(ticket));
                deleteBtn.setOnAction(e -> handleDelete(ticket));
                setGraphic(box);
            }
        });
    }

    private Button createIconButton(String iconType, String color) {
        Button btn = new Button();
        btn.setMinSize(38, 38);
        btn.setMaxSize(38, 38);

        SVGPath icon = new SVGPath();
        switch (iconType) {
            case "eye" ->
                    icon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z");
            case "qrcode" ->
                    icon.setContent("M4 4h6v6H4V4zm10 0h6v6h-6V4zM4 14h6v6H4v-6zm14 0h-4v2h2v4h2v-4h2v-6h-2v4z");
            case "trash" ->
                    icon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
            default -> icon.setContent("");
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
        } catch (Exception e) {
            System.err.println("Erreur chargement tickets: " + e.getMessage());
            allTickets = List.of();
            filteredTickets = List.of();
            if (ticketTable != null) {
                ticketTable.getItems().clear();
            }
        }
    }

    private void populateEventFilter() {
        if (eventFilter == null) {
            return;
        }

        eventFilter.getItems().clear();
        eventFilter.getItems().add("Tous les evenements");

        try {
            allEvents = eventService.getAllEvents();
            List<Integer> eventIdsWithTickets = allTickets.stream()
                    .map(EventTicket::getEventId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            for (Integer eventId : eventIdsWithTickets) {
                Event event = eventService.getEventById(eventId);
                if (event != null && event.getTitle() != null) {
                    eventFilter.getItems().add(event.getTitle());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur filtre evenements: " + e.getMessage());
        }

        eventFilter.setValue("Tous les evenements");
    }

    private void updateStatistics() {
        int total = allTickets != null ? allTickets.size() : 0;
        long used = allTickets != null ? allTickets.stream().filter(EventTicket::isUsed).count() : 0;

        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(total));
        }
        if (usedLabel != null) {
            usedLabel.setText(String.valueOf(used));
        }
    }

    private void applyFilters() {
        if (allTickets == null) {
            return;
        }

        String searchText = searchField != null && searchField.getText() != null
                ? searchField.getText().toLowerCase().trim()
                : "";
        String eventName = eventFilter != null ? eventFilter.getValue() : null;
        String status = statusFilter != null ? statusFilter.getValue() : null;

        filteredTickets = allTickets.stream()
                .filter(ticket -> {
                    boolean matchSearch = searchText.isEmpty()
                            || ticket.getTicketCode().toLowerCase().contains(searchText);

                    boolean matchEvent = eventName == null || "Tous les evenements".equals(eventName);
                    if (!matchEvent) {
                        matchEvent = getEventName(ticket.getEventId()).equals(eventName);
                    }

                    boolean matchStatus = status == null || "Tous".equals(status)
                            || ("Utilise".equals(status) && ticket.isUsed())
                            || ("Non utilise".equals(status) && !ticket.isUsed());

                    return matchSearch && matchEvent && matchStatus;
                })
                .toList();

        currentPage = 1;
        if (resultLabel != null) {
            resultLabel.setText(filteredTickets.size() + " resultat(s) trouve(s)");
        }
        setupPagination();
    }

    @FXML
    private void handleAdd() {
        int eventId = resolveEventIdForCreation();
        int userId = resolveUserIdForCreation();

        if (eventId <= 0) {
            showError("Erreur", "Aucun evenement disponible pour creer un ticket.");
            return;
        }
        if (userId <= 0) {
            showError("Erreur", "Aucun utilisateur valide trouve pour creer un ticket.");
            return;
        }

        EventTicket ticket = ticketService.createTicket(eventId, userId);
        if (ticket != null) {
            loadTickets();
            showSuccess("Succes", "Ticket cree: " + ticket.getTicketCode());
        } else {
            showError("Erreur", "Impossible de creer le ticket.");
        }
    }

    private int resolveEventIdForCreation() {
        if (eventFilter != null) {
            String selectedEvent = eventFilter.getValue();
            if (selectedEvent != null && allEvents != null) {
                for (Event event : allEvents) {
                    if (selectedEvent.equals(event.getTitle())) {
                        return event.getId();
                    }
                }
            }
        }
        return (allEvents != null && !allEvents.isEmpty()) ? allEvents.get(0).getId() : -1;
    }

    private int resolveUserIdForCreation() {
        int sessionUserId = UserSession.getInstance().getUserId();
        if (sessionUserId > 0) {
            return sessionUserId;
        }

        try {
            List<UserModel> users = userService.getAllUsers();
            if (users != null && !users.isEmpty()) {
                return users.get(0).getId_User();
            }
        } catch (Exception e) {
            System.err.println("Erreur recuperation utilisateur: " + e.getMessage());
        }
        return -1;
    }

    @FXML
    private void handleScan() {
        showError("Information", "Le scan global n'est pas encore implemente.");
    }

    private void handleView(EventTicket ticket) {
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Details du ticket");
        details.setHeaderText("Ticket: " + ticket.getTicketCode());
        details.setContentText(String.format(
                "Evenement ID: %d%nUtilisateur ID: %d%nStatut: %s%nCree le: %s%nUtilise le: %s",
                ticket.getEventId(),
                ticket.getUserId(),
                ticket.isUsed() ? "Utilise" : "Non utilise",
                ticket.getFormattedCreatedAt(),
                ticket.getFormattedUsedAt()
        ));
        details.showAndWait();
    }

    private void handleScan(EventTicket ticket) {
        if (ticket.isUsed()) {
            showError("Erreur", "Ce ticket a deja ete utilise.");
            return;
        }

        boolean success = ticketService.markTicketAsUsed(ticket.getId());
        if (success) {
            showSuccess("Succes", "Ticket scanne et marque comme utilise.");
            loadTickets();
        } else {
            showError("Erreur", "Impossible de scanner le ticket.");
        }
    }

    private void handleDelete(EventTicket ticket) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le ticket " + ticket.getTicketCode() + " ?");
        confirm.setContentText("Cette action est irreversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (ticketService.deleteTicket(ticket.getId())) {
                    showSuccess("Succes", "Ticket supprime.");
                    loadTickets();
                } else {
                    showError("Erreur", "Suppression impossible.");
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
            System.err.println("Erreur recuperation utilisateur " + userId + ": " + e.getMessage());
        }
        return "Utilisateur #" + userId;
    }

    private String getEventName(int eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            if (event != null && event.getTitle() != null && !event.getTitle().isBlank()) {
                return event.getTitle();
            }
        } catch (Exception e) {
            System.err.println("Erreur recuperation evenement " + eventId + ": " + e.getMessage());
        }
        return "Evenement #" + eventId;
    }

    private void setupPagination() {
        if (filteredTickets == null || filteredTickets.isEmpty()) {
            totalPages = 1;
            currentPage = 1;
            if (ticketTable != null) {
                ticketTable.getItems().clear();
            }
            updatePaginationUI();
            return;
        }

        totalPages = (int) Math.ceil((double) filteredTickets.size() / itemsPerPage);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        displayCurrentPage();
        updatePaginationUI();
    }

    private void displayCurrentPage() {
        if (ticketTable == null) {
            return;
        }
        if (filteredTickets == null || filteredTickets.isEmpty()) {
            ticketTable.getItems().clear();
            return;
        }

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredTickets.size());
        ticketTable.getItems().setAll(filteredTickets.subList(fromIndex, toIndex));
    }

    private void updatePaginationUI() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + currentPage + " sur " + totalPages);
        }

        if (prevBtn != null) {
            prevBtn.setDisable(currentPage <= 1);
        }
        if (paginationContainer == null) {
            return;
        }

        paginationContainer.getChildren().clear();

        int maxButtons = 5;
        int startPage;
        int endPage;
        if (totalPages <= maxButtons) {
            startPage = 1;
            endPage = totalPages;
        } else {
            int half = maxButtons / 2;
            if (currentPage <= half + 1) {
                startPage = 1;
                endPage = maxButtons;
            } else if (currentPage >= totalPages - half) {
                startPage = totalPages - maxButtons + 1;
                endPage = totalPages;
            } else {
                startPage = currentPage - half;
                endPage = currentPage + half;
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            int page = i;
            Button pageBtn = new Button(String.valueOf(i));
            pageBtn.setOnAction(e -> goToPage(page));
            if (i == currentPage) {
                pageBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            }
            paginationContainer.getChildren().add(pageBtn);
        }

        Button nextBtn = new Button("Suivant");
        nextBtn.setDisable(currentPage >= totalPages);
        nextBtn.setOnAction(e -> handleNextPage());
        paginationContainer.getChildren().add(nextBtn);
    }

    private void goToPage(int pageNum) {
        if (pageNum < 1 || pageNum > totalPages || pageNum == currentPage) {
            return;
        }
        currentPage = pageNum;
        displayCurrentPage();
        updatePaginationUI();
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
