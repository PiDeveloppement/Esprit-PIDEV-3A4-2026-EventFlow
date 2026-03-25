package com.example.pidev.controller.event;

import com.example.pidev.MainController;

import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventCategoryService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Controller pour la liste des événements
 * @author Ons Abdesslem
 */
public class EventListController {

    // ========== TABLEAU ==========
    @FXML private TableView<Event> eventTable;
    @FXML private TableColumn<Event, String> titleCol;
    @FXML private TableColumn<Event, String> startDateCol;
    @FXML private TableColumn<Event, String> endDateCol;
    @FXML private TableColumn<Event, String> locationCol;
    @FXML private TableColumn<Event, String> categoryCol;
    @FXML private TableColumn<Event, String> statusCol;
    @FXML private TableColumn<Event, String> priceCol;
    @FXML private TableColumn<Event, Void> actionsCol;

    // ========== FILTRES ==========
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortOrder;

    // ========== KPI ==========
    @FXML private Label totalLabel;
    @FXML private Label upcomingLabel;
    @FXML private Label resultLabel;

    // ========== NAVBAR ==========
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    // ========== BOUTONS ==========
    @FXML private Button addBtn;

    // ========== PAGINATION ==========
    @FXML private HBox paginationContainer;
    @FXML private Button prevBtn;
    @FXML private Label pageInfoLabel;

    private EventService eventService;
    private EventCategoryService categoryService;
    private MainController helloController;
    private List<Event> allEvents;
    private List<Event> filteredEvents;
    private List<EventCategory> allCategories;

    // Variables de pagination
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;


    @FXML
    public void initialize() {
        System.out.println("✅ EventListController initialisé");

        eventService = new EventService();
        categoryService = new EventCategoryService();

        setupFilters();
        setupTableColumns();
        loadEvents();

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

    /**
     * Configuration des filtres
     */
    private void setupFilters() {
        // ============ RECHERCHE ============
        searchField.setPromptText("Rechercher un événement par titre, lieu ou description...");
        searchField.textProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });

        // ============ FILTRE STATUT ============
        statusFilter.getItems().addAll("Tous les statuts", "À venir", "En cours", "Terminé");
        statusFilter.setValue("Tous les statuts");
        statusFilter.valueProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });

        // ============ FILTRE CATÉGORIE ============
        categoryFilter.getItems().add("Toutes les catégories");
        categoryFilter.setValue("Toutes les catégories");
        configureCategoryFilterCells(); // Configuration avec icônes
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });

        // ============ TRI (ORDRE) ============
        sortOrder.getItems().addAll("Plus récents", "Plus anciens", "A → Z", "Z → A");
        sortOrder.setValue("Plus récents");
        sortOrder.valueProperty().addListener((obs, old, newVal) -> {
            currentPage = 1;
            applyFilters();
        });

        System.out.println("✅ Filtres configurés");
    }

    /**
     * Configure l'affichage du filtre catégorie avec icônes
     */
    private void configureCategoryFilterCells() {
        // CellFactory pour la liste déroulante
        categoryFilter.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                if ("Toutes les catégories".equals(item)) {
                    setGraphic(null);
                    setText(item);
                    return;
                }

                // Trouver la catégorie correspondante
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

        // ButtonCell pour le bouton (affichage de la sélection)
        categoryFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Toutes les catégories");
                    return;
                }

                if ("Toutes les catégories".equals(item)) {
                    setText(item);
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

    /**
     * Trouve une catégorie par son nom
     */
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

    private void setupTableColumns() {
        // Titre
        titleCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getTitle())
        );
        // Configure la colonne Titre avec wrap text pour afficher le texte sur plusieurs lignes
        titleCol.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setStyle("-fx-text-fill: #212529; -fx-font-size: 13px; -fx-font-weight: 600;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });

        // Date début
        startDateCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().getFormattedStartDate()
                )
        );

        // Date fin
        endDateCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().getFormattedEndDate()
                )
        );

        // Lieu
        locationCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getLocation())
        );
        // Configure la colonne Lieu avec wrap text pour afficher le texte sur plusieurs lignes
        locationCol.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(label);
                }
            }
        });

        // Catégorie (dynamique avec icône)
        categoryCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(getCategoryName(param.getValue().getCategoryId()))
        );

        // Statut avec badge
        statusCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(getEventStatus(param.getValue()))
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

                    switch (status) {
                        case "À venir":
                            badge.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; " +
                                    "-fx-padding: 4 12; -fx-background-radius: 20;");
                            break;
                        case "En cours":
                            badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; " +
                                    "-fx-padding: 4 12; -fx-background-radius: 20;");
                            break;
                        case "Terminé":
                            badge.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563; " +
                                    "-fx-padding: 4 12; -fx-background-radius: 20;");
                            break;
                    }

                    setGraphic(badge);
                }
            }
        });

        // Prix
        priceCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getPriceDisplay())
        );

        // Actions (voir, modifier, supprimer)
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = createIconButton("eye", "#17a2b8");
            private final Button editBtn = createIconButton("edit", "#0d6efd");
            private final Button deleteBtn = createIconButton("trash", "#dc3545");
            private final HBox container = new HBox(8, viewBtn, editBtn, deleteBtn);
            {
                container.setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Event event = getTableView().getItems().get(getIndex());
                    viewBtn.setOnAction(e -> handleView(event));
                    editBtn.setOnAction(e -> handleEdit(event));
                    deleteBtn.setOnAction(e -> handleDelete(event));
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
            case "edit":
                icon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
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

    private void loadEvents() {
        try {
            allEvents = eventService.getAllEvents();
            allCategories = categoryService.getAllCategories();
            populateCategoryFilter();
            applyFilters();
            updateStatistics();
            System.out.println("✅ " + allEvents.size() + " événements chargés");
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());

        }
    }

    private void populateCategoryFilter() {
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("Toutes les catégories");

        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                categoryFilter.getItems().add(cat.getName());
            }
        }

        System.out.println("✅ Filtre catégorie mis à jour avec " + (allCategories != null ? allCategories.size() + 1 : 1) + " options");
    }

    private String getCategoryName(int categoryId) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getId() == categoryId) {
                    return cat.getName();
                }
            }
        }
        return "Catégorie " + categoryId;
    }

    /**
     * Retourne le statut temporel de l'événement
     */
    private String getEventStatus(Event event) {
        LocalDateTime now = LocalDateTime.now();

        if (event.getStartDate() == null || event.getEndDate() == null) {
            return "À venir"; // Par défaut
        }

        if (event.getEndDate().isBefore(now)) {
            return "Terminé";
        } else if (event.getStartDate().isAfter(now)) {
            return "À venir";
        } else if (now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate())) {
            return "En cours";
        }

        return "À venir";
    }

    private void updateStatistics() {
        if (allEvents == null) return;
        int total = allEvents.size();
        long upcoming = allEvents.stream()
                .filter(e -> e.getStartDate() != null && e.getStartDate().isAfter(LocalDateTime.now()))
                .count();
        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(total));
        }
        if (upcomingLabel != null) {
            upcomingLabel.setText(String.valueOf(upcoming));
        }
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    private void applyFilters() {
        if (allEvents == null) return;

        String searchText = searchField.getText().toLowerCase().trim();
        String status = statusFilter.getValue();
        String category = categoryFilter.getValue();
        String sort = sortOrder.getValue();

        List<Event> filtered = allEvents.stream()
                .filter(event -> {
                    // Recherche
                    boolean matchSearch = searchText.isEmpty() ||
                            event.getTitle().toLowerCase().contains(searchText) ||
                            (event.getDescription() != null && event.getDescription().toLowerCase().contains(searchText)) ||
                            (event.getLocation() != null && event.getLocation().toLowerCase().contains(searchText));

                    // Filtre statut
                    boolean matchStatus = status == null || "Tous les statuts".equals(status);
                    if (!matchStatus) {
                        String eventStatus = getEventStatus(event);
                        matchStatus = eventStatus.equals(status);
                    }

                    // Filtre catégorie
                    boolean matchCategory = category == null || "Toutes les catégories".equals(category);
                    if (!matchCategory) {
                        String categoryName = getCategoryName(event.getCategoryId());
                        matchCategory = categoryName.equals(category);
                    }

                    return matchSearch && matchStatus && matchCategory;
                })
                .collect(Collectors.toList());

        // Tri
        if (sort != null) {
            switch (sort) {
                case "Plus récents":
                    filtered.sort(Comparator.comparing(Event::getStartDate).reversed());
                    break;
                case "Plus anciens":
                    filtered.sort(Comparator.comparing(Event::getStartDate));
                    break;
                case "A → Z":
                    filtered.sort(Comparator.comparing(Event::getTitle));
                    break;
                case "Z → A":
                    filtered.sort(Comparator.comparing(Event::getTitle).reversed());
                    break;
            }
        } else {
            filtered.sort(Comparator.comparing(Event::getStartDate).reversed());
        }

        filteredEvents = filtered;
        currentPage = 1;
        resultLabel.setText(filtered.size() + " résultat(s) trouvé(s)");
        setupPagination();
    }

    @FXML
    private void handleAdd() {
        System.out.println("[EventList] handleAdd() appelé");
        if (helloController == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de navigation");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.showAndWait();
            return;
        }
        helloController.showEventForm(null);
    }

    /**
     * Ouvre la vue calendrier
     */
    @FXML
    private void handleShowCalendar() {
        System.out.println("📅 Ouverture du calendrier des événements");
        if (helloController != null) {
            helloController.showEventCalendar();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le calendrier");
            alert.setContentText("Le contrôleur principal n'est pas disponible");
            alert.showAndWait();
        }
    }

    private void handleView(Event event) {
        System.out.println("👁️ Consultation de: " + event.getTitle());
        if (helloController != null) {
            helloController.showEventView(event);
        } else {
            Alert details = new Alert(Alert.AlertType.INFORMATION);
            details.setTitle("Détails");
            details.setHeaderText(event.getDisplayName());
            details.setContentText(String.format(
                    "Description: %s\n\nLieu: %s\n\nDate: %s - %s\n\nCapacité: %d\n\nPrix: %s",
                    event.getDescription(), event.getLocation(),
                    event.getFormattedStartDate(), event.getFormattedEndDate(),
                    event.getCapacity(), event.getPriceDisplay()
            ));
            details.showAndWait();
        }
    }

    private void handleEdit(Event event) {
        System.out.println("✏️ Modification de: " + event.getTitle());
        if (helloController != null) {
            helloController.showEventForm(event);
        }
    }

    private void handleDelete(Event event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + event.getTitle() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (eventService.deleteEvent(event.getId())) {
                    showSuccess("Succès", "Événement supprimé !");
                    loadEvents();

                    // Rafraîchir les KPI après suppression
                    if (helloController != null) {
                        helloController.refreshKPIs();
                    }
                } else {
                    showError("Erreur", "Impossible de supprimer");
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
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
        if (filteredEvents == null || filteredEvents.isEmpty()) {
            totalPages = 1;
            currentPage = 1;
            updatePaginationUI();
            return;
        }

        totalPages = (int) Math.ceil((double) filteredEvents.size() / itemsPerPage);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        updatePaginationUI();
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        if (filteredEvents == null || filteredEvents.isEmpty()) {
            eventTable.getItems().clear();
            return;
        }

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredEvents.size());

        List<Event> pageItems = filteredEvents.subList(fromIndex, toIndex);
        eventTable.getItems().clear();
        eventTable.getItems().addAll(pageItems);
    }

    private void updatePaginationUI() {
        pageInfoLabel.setText("Page " + currentPage + " sur " + totalPages);
        paginationContainer.getChildren().clear();
        prevBtn.setDisable(currentPage == 1);
        paginationContainer.getChildren().add(prevBtn);

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
            } else {
                pageBtn.getStyleClass().add("btn-page");
            }
            pageBtn.setOnAction(e -> goToPage(pageNum));
            paginationContainer.getChildren().add(pageBtn);
        }

        if (endPage < totalPages) {
            Label dots = new Label("...");
            dots.setStyle("-fx-text-fill: #6c757d; -fx-padding: 5 10;");
            paginationContainer.getChildren().add(dots);
            Button lastPageBtn = new Button(String.valueOf(totalPages));
            lastPageBtn.getStyleClass().add("btn-page");
            lastPageBtn.setOnAction(e -> goToPage(totalPages));
            paginationContainer.getChildren().add(lastPageBtn);
        }

        Button nextBtn = new Button("Suivant »");
        nextBtn.getStyleClass().add("btn-pagination");
        nextBtn.setDisable(currentPage == totalPages);
        nextBtn.setOnAction(e -> handleNextPage());
        paginationContainer.getChildren().add(nextBtn);
    }

    private void goToPage(int pageNum) {
        if (pageNum >= 1 && pageNum <= totalPages) {
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

    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            displayCurrentPage();
            updatePaginationUI();
        }
    }

    /**
     * Export des événements en PDF
     */
    @FXML
    private void handleExportPDF() {
        try {
            // Ouvrir un FileChooser pour choisir l'emplacement
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");

            // Nom du fichier avec date et heure
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            fileChooser.setInitialFileName("evenements_" + timestamp + ".pdf");

            // Filtrer uniquement les PDF
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
            );

            // Afficher le dialogue
            File file = fileChooser.showSaveDialog(eventTable.getScene().getWindow());

            if (file != null) {
                // Créer le PDF
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                // En-tête
                Paragraph header = new Paragraph("EventFlow - Liste des Événements")
                        .setFontSize(20)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(13, 71, 161));
                document.add(header);

                // Date d'exportation
                String dateExport = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm", Locale.FRENCH)
                );
                Paragraph dateInfo = new Paragraph("Exporté le " + dateExport)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.GRAY);
                document.add(dateInfo);

                // Espacement
                document.add(new Paragraph("\n"));

                // Statistiques
                int totalEvts = allEvents.size();
                long upcoming = allEvents.stream()
                        .filter(e -> e.getStartDate().isAfter(LocalDateTime.now()))
                        .count();
                long ongoing = allEvents.stream()
                        .filter(e -> e.getStartDate().isBefore(LocalDateTime.now()) &&
                                e.getEndDate().isAfter(LocalDateTime.now()))
                        .count();
                long finished = totalEvts - upcoming - ongoing;

                Paragraph stats = new Paragraph(
                        String.format("Total: %d événements | À venir: %d | En cours: %d | Terminés: %d",
                                totalEvts, upcoming, ongoing, finished))
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setItalic();
                document.add(stats);

                document.add(new Paragraph("\n"));

                // Créer le tableau
                float[] columnWidths = {3, 2, 2, 3, 2, 2, 1.5f};
                Table table = new Table(UnitValue.createPercentArray(columnWidths))
                        .useAllAvailableWidth();

                // En-têtes de colonnes
                String[] headers = {"Titre", "Début", "Fin", "Lieu", "Catégorie", "Statut", "Prix"};
                for (String headerText : headers) {
                    Cell headerCell = new Cell()
                            .add(new Paragraph(headerText).setBold())
                            .setBackgroundColor(new DeviceRgb(33, 150, 243))
                            .setFontColor(ColorConstants.WHITE)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(8);
                    table.addHeaderCell(headerCell);
                }

                // Ajouter les données
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

                for (Event event : allEvents) {
                    // Titre
                    table.addCell(new Cell()
                            .add(new Paragraph(event.getTitle()))
                            .setPadding(6));

                    // Date début
                    String startDate = event.getStartDate() != null
                            ? event.getStartDate().format(dateFormatter)
                            : "-";
                    table.addCell(new Cell()
                            .add(new Paragraph(startDate))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));

                    // Date fin
                    String endDate = event.getEndDate() != null
                            ? event.getEndDate().format(dateFormatter)
                            : "-";
                    table.addCell(new Cell()
                            .add(new Paragraph(endDate))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));

                    // Lieu
                    String location = event.getLocation();
                    if (location == null || location.isEmpty()) location = "-";
                    if (location.length() > 40) location = location.substring(0, 37) + "...";
                    table.addCell(new Cell()
                            .add(new Paragraph(location))
                            .setPadding(6));

                    // Catégorie
                    String category = getCategoryName(event.getCategoryId());
                    table.addCell(new Cell()
                            .add(new Paragraph(category))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));

                    // Statut
                    String status;
                    DeviceRgb statusColor;
                    LocalDateTime now = LocalDateTime.now();

                    if (event.getStartDate().isAfter(now)) {
                        status = "À venir";
                        statusColor = new DeviceRgb(33, 150, 243); // Bleu
                    } else if (event.getEndDate().isAfter(now)) {
                        status = "En cours";
                        statusColor = new DeviceRgb(76, 175, 80); // Vert
                    } else {
                        status = "Terminé";
                        statusColor = new DeviceRgb(158, 158, 158); // Gris
                    }

                    table.addCell(new Cell()
                            .add(new Paragraph(status))
                            .setFontColor(statusColor)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));

                    // Prix
                    String price = !event.isFree() && event.getTicketPrice() > 0
                            ? String.format("%.2f €", event.getTicketPrice())
                            : "Gratuit";
                    table.addCell(new Cell()
                            .add(new Paragraph(price))
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setPadding(6));
                }

                document.add(table);

                // Footer
                document.add(new Paragraph("\n"));
                Paragraph footer = new Paragraph("© 2026 EventFlow - Gestion des événements")
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.LIGHT_GRAY);
                document.add(footer);

                // Fermer le document
                document.close();

                // Afficher un message de succès
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export réussi");
                alert.setHeaderText(null);
                alert.setContentText("Le PDF a été exporté avec succès !\n\n" +
                        totalEvts + " événements exportés.\nFichier: " + file.getName());
                alert.showAndWait();

                System.out.println("✅ PDF exporté: " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur export PDF: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'export");
            alert.setHeaderText("Impossible d'exporter le PDF");
            alert.setContentText("Erreur: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
