package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.EventCategory;
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
 * Controller pour la liste des catÃ©gories - Version avec recherche corrigÃ©e
 * @author Ons Abdesslem
 */
public class CategoryListController {

    // ========== TABLEAU ==========
    @FXML private TableView<EventCategory> categoryTable;
    @FXML private TableColumn<EventCategory, EventCategory> categoryCol;
    @FXML private TableColumn<EventCategory, String> descriptionCol;
    @FXML private TableColumn<EventCategory, String> colorCol;
    @FXML private TableColumn<EventCategory, Boolean> statusCol;
    @FXML private TableColumn<EventCategory, Void> actionsCol;

    // ========== FILTRES ==========
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> colorFilter;
    @FXML private ComboBox<String> sortOrder;

    // ========== KPI ==========
    @FXML private Label totalLabel;
    @FXML private Label eventsLabel;
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

    private EventCategoryService categoryService;
    private MainController helloController;
    private List<EventCategory> allCategories;
    private List<EventCategory> filteredCategories;

    // Variables de pagination
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;


    @FXML
    public void initialize() {
        System.out.println("âœ… CategoryListController initialisÃ©");

        categoryService = new EventCategoryService();

        setupFilters();
        setupTableColumns();
        loadCategories();

        // ========== DATE/HEURE TEMPS RÃ‰EL ==========
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
        if (searchField != null) {
            searchField.setPromptText("Rechercher une catÃ©gorie...");
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("ðŸ” Recherche changÃ©e: '" + newVal + "' (ancienne: '" + oldVal + "')");
                currentPage = 1;
                applyFilters();
            });
        }

        // ============ FILTRE STATUT ============
        if (statusFilter != null) {
            statusFilter.getItems().addAll("Tous les statuts", "Actif", "Inactif");
            statusFilter.setValue("Tous les statuts");
            statusFilter.valueProperty().addListener((obs, old, newVal) -> {
                System.out.println("ðŸ“Š Filtre statut changÃ©: " + newVal);
                currentPage = 1;
                applyFilters();
            });
        }

        // ============ FILTRE COULEUR ============
        if (colorFilter != null) {
            colorFilter.getItems().add("Toutes les couleurs");
            configureColorFilterCells();
            colorFilter.setValue("Toutes les couleurs");
            colorFilter.valueProperty().addListener((obs, old, newVal) -> {
                System.out.println("ðŸŽ¨ Filtre couleur changÃ©: " + newVal);
                currentPage = 1;
                applyFilters();
            });
        }

        // ============ TRI (ORDRE) ============
        if (sortOrder != null) {
            sortOrder.getItems().addAll("A â†’ Z", "Z â†’ A", "Plus rÃ©cents", "Plus anciens");
            sortOrder.setValue("A â†’ Z");
            sortOrder.valueProperty().addListener((obs, old, newVal) -> {
                System.out.println("ðŸ”¤ Tri changÃ©: " + newVal);
                currentPage = 1;
                applyFilters();
            });
        }

        System.out.println("âœ… Filtres configurÃ©s");
    }

    /**
     * Configure les cellules du filtre COULEUR avec affichage personnalisÃ©
     */
    private void configureColorFilterCells() {
        colorFilter.setCellFactory(listView -> new ListCell<>() {
            private final Circle colorCircle = new Circle(7);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                if ("Toutes les couleurs".equals(item)) {
                    setGraphic(null);
                    setText(item);
                    setStyle("-fx-text-fill: #495057;");
                    return;
                }

                String colorHex = extractColorHex(item);
                if (colorHex != null) {
                    try {
                        colorCircle.setFill(Color.web(colorHex));
                        colorCircle.setStroke(Color.web("#dee2e6"));
                        colorCircle.setStrokeWidth(1.2);
                        HBox box = new HBox(8, colorCircle, new Label(item));
                        box.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(box);
                        setText(null);
                    } catch (Exception e) {
                        setGraphic(null);
                        setText(item);
                    }
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        });

        colorFilter.setButtonCell(new ListCell<>() {
            private final Circle colorCircle = new Circle(7);

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText("Toutes les couleurs");
                    setStyle("-fx-text-fill: #495057;");
                    return;
                }

                if ("Toutes les couleurs".equals(item)) {
                    setGraphic(null);
                    setText(item);
                    setStyle("-fx-text-fill: #495057;");
                    return;
                }

                String colorHex = extractColorHex(item);
                if (colorHex != null) {
                    try {
                        colorCircle.setFill(Color.web(colorHex));
                        colorCircle.setStroke(Color.web("#dee2e6"));
                        colorCircle.setStrokeWidth(1.2);
                        HBox box = new HBox(8, colorCircle, new Label(item));
                        box.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(box);
                        setText(null);
                    } catch (Exception e) {
                        setGraphic(null);
                        setText(item);
                    }
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        });
    }

    private void setupTableColumns() {
        // CatÃ©gorie avec icÃ´ne
        categoryCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleObjectProperty<>(param.getValue())
        );
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(EventCategory cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setGraphic(null);
                } else {
                    Label icon = new Label(cat.getIcon() != null ? cat.getIcon() : "ðŸ“Œ");
                    icon.setStyle("-fx-font-size: 22px;");
                    Label name = new Label(cat.getName());
                    name.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                    HBox box = new HBox(10, icon, name);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        // Description
        descriptionCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(
                        param.getValue().getDescription() != null ? param.getValue().getDescription() : ""
                )
        );
        descriptionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String desc, boolean empty) {
                super.updateItem(desc, empty);
                if (empty || desc == null || desc.isEmpty()) {
                    setText(null);
                } else {
                    setText(desc);
                    setWrapText(true);
                    setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px; -fx-padding: 8 10;");
                }
            }
        });

        // Couleur avec cercle
        colorCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getColor())
        );
        colorCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String colorHex, boolean empty) {
                super.updateItem(colorHex, empty);
                if (empty || colorHex == null) {
                    setGraphic(null);
                } else {
                    Circle circle = new Circle(16);
                    try {
                        circle.setFill(Color.web(colorHex));
                        circle.setStroke(Color.web("#dee2e6"));
                        circle.setStrokeWidth(2);
                    } catch (Exception e) {
                        circle.setFill(Color.GRAY);
                    }
                    Label hex = new Label(colorHex);
                    hex.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057; -fx-font-family: monospace;");
                    HBox box = new HBox(10, circle, hex);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        // Statut avec badge
        statusCol.setCellValueFactory(param ->
                new javafx.beans.property.SimpleObjectProperty<>(param.getValue().isActive())
        );
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(active ? "Actif" : "Inactif");
                    badge.getStyleClass().add("status-badge");
                    badge.getStyleClass().add(active ? "status-badge-active" : "status-badge-inactive");
                    setGraphic(badge);
                }
            }
        });

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
                    EventCategory category = getTableView().getItems().get(getIndex());
                    viewBtn.setOnAction(e -> handleView(category));
                    editBtn.setOnAction(e -> handleEdit(category));
                    deleteBtn.setOnAction(e -> handleDelete(category));
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

    private void loadCategories() {
        try {
            allCategories = categoryService.getAllCategoriesWithCount();
            populateColorFilter();
            applyFilters();
            updateStatistics();
            System.out.println("âœ… " + allCategories.size() + " catÃ©gories chargÃ©es");
        } catch (Exception e) {
            System.err.println("âŒ Erreur: " + e.getMessage());
        }
    }

    private void populateColorFilter() {
        List<String> colors = allCategories.stream()
                .map(EventCategory::getColor)
                .filter(color -> color != null && !color.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        String currentValue = colorFilter.getValue();
        colorFilter.getItems().clear();
        colorFilter.getItems().add("Toutes les couleurs");
        for (String colorHex : colors) {
            colorFilter.getItems().add(getColorName(colorHex));
        }

        if (currentValue != null && colorFilter.getItems().contains(currentValue)) {
            colorFilter.setValue(currentValue);
        } else {
            colorFilter.setValue("Toutes les couleurs");
        }

        System.out.println("âœ… Filtre couleur mis Ã  jour avec " + (colors.size() + 1) + " options");
    }

    private String extractColorHex(String colorText) {
        if (colorText == null) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("#[0-9A-Fa-f]{6}")
                .matcher(colorText);
        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private void updateStatistics() {
        if (allCategories == null) return;
        int total = allCategories.size();
        int events = allCategories.stream().mapToInt(EventCategory::getEventCount).sum();
        totalLabel.setText(String.valueOf(total));
        eventsLabel.setText(String.valueOf(events));
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    /**
     * MÃ©thode de filtrage avec la recherche de l'ancien code (qui fonctionnait)
     */
    private void applyFilters() {
        if (allCategories == null || allCategories.isEmpty()) {
            System.out.println("âš ï¸ Aucune catÃ©gorie chargÃ©e");
            return;
        }

        String searchText = searchField.getText().toLowerCase().trim();
        String status = statusFilter.getValue();
        String color = colorFilter.getValue();
        String sort = sortOrder.getValue();

        String selectedColorHex = extractColorHex(color);

        // ========== LOGS DE DIAGNOSTIC ==========
        if (!searchText.isEmpty()) {
            System.out.println("\nðŸ”Ž DIAGNOSTIC DE RECHERCHE");
            System.out.println("   Texte saisi: '" + searchField.getText() + "'");
            System.out.println("   Texte traitÃ©: '" + searchText + "'");
            System.out.println("   Total catÃ©gories: " + allCategories.size());
        }

        List<EventCategory> filtered = allCategories.stream()
                .filter(cat -> {
                    // RECHERCHE - cherche au DÃ‰BUT du nom (startsWith) et dans la description
                    boolean matchSearch = searchText.isEmpty() ||
                            cat.getName().toLowerCase().startsWith(searchText) ||
                            (cat.getDescription() != null && cat.getDescription().toLowerCase().startsWith(searchText));

                    boolean matchStatus = status == null || "Tous les statuts".equals(status) ||
                            (status.equals("Actif") && cat.isActive()) ||
                            (status.equals("Inactif") && !cat.isActive());

                    boolean matchColor = color == null || "Toutes les couleurs".equals(color) ||
                            (selectedColorHex != null && cat.getColor() != null &&
                                    cat.getColor().equalsIgnoreCase(selectedColorHex));

                    // Log dÃ©taillÃ© pour chaque catÃ©gorie
                    if (!searchText.isEmpty()) {
                        boolean nameMatch = cat.getName().toLowerCase().startsWith(searchText);
                        boolean descMatch = cat.getDescription() != null &&
                                cat.getDescription().toLowerCase().startsWith(searchText);
                        if (matchSearch) {
                            String source = nameMatch ? "(nom)" : descMatch ? "(desc)" : "(vide)";
                            System.out.println("   âœ… '" + cat.getName() + "' " + source);
                        }
                    }

                    return matchSearch && matchStatus && matchColor;
                })
                .collect(Collectors.toList());

        if (!searchText.isEmpty()) {
            System.out.println("   ðŸ“Š RÃ©sultat: " + filtered.size() + "/" + allCategories.size() + " catÃ©gories\n");
        }

        if (sort != null) {
            switch (sort) {
                case "A â†’ Z": filtered.sort(Comparator.comparing(EventCategory::getName)); break;
                case "Z â†’ A": filtered.sort(Comparator.comparing(EventCategory::getName).reversed()); break;
                case "Plus rÃ©cents": filtered.sort(Comparator.comparing(EventCategory::getCreatedAt).reversed()); break;
                case "Plus anciens": filtered.sort(Comparator.comparing(EventCategory::getCreatedAt)); break;
            }
        } else {
            filtered.sort(Comparator.comparing(EventCategory::getName));
        }

        filteredCategories = filtered;
        currentPage = 1;
        resultLabel.setText(filtered.size() + " rÃ©sultat(s) trouvÃ©(s)");
        setupPagination();
    }

    @FXML
    private void handleAdd() {
        System.out.println("[CategoryList] handleAdd() appelÃ©");
        if (helloController == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de navigation");
            alert.setHeaderText("Impossible d'ouvrir le formulaire");
            alert.showAndWait();
            return;
        }
        helloController.showCategoryForm(null);
    }

    private void handleView(EventCategory category) {
        System.out.println("ðŸ‘ï¸ Consultation de: " + category.getName());
        if (helloController != null) {
            helloController.showCategoryView(category);
        } else {
            Alert details = new Alert(Alert.AlertType.INFORMATION);
            details.setTitle("DÃ©tails");
            details.setHeaderText(category.getDisplayName());
            details.setContentText(String.format(
                    "Description: %s\n\nCouleur: %s\n\nStatut: %s\n\nÃ‰vÃ©nements: %d",
                    category.getDescription(), category.getColor(),
                    category.isActive() ? "âœ… Actif" : "âŒ Inactif",
                    category.getEventCount()
            ));
            details.showAndWait();
        }
    }

    private void handleEdit(EventCategory category) {
        System.out.println("âœï¸ Modification de: " + category.getName());
        if (helloController != null) {
            helloController.showCategoryForm(category);
        }
    }

    private void handleDelete(EventCategory category) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer " + category.getName() + " ?");
        confirm.setContentText("Cette action est irrÃ©versible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (categoryService.deleteCategory(category.getId())) {
                    showSuccess("SuccÃ¨s", "CatÃ©gorie supprimÃ©e !");
                    loadCategories();

                    // RafraÃ®chir les KPI aprÃ¨s suppression
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
        loadCategories();
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
        if (filteredCategories == null || filteredCategories.isEmpty()) {
            totalPages = 1;
            currentPage = 1;
            updatePaginationUI();
            return;
        }

        totalPages = (int) Math.ceil((double) filteredCategories.size() / itemsPerPage);
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        updatePaginationUI();
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        if (filteredCategories == null || filteredCategories.isEmpty()) {
            categoryTable.getItems().clear();
            return;
        }

        int fromIndex = (currentPage - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, filteredCategories.size());

        List<EventCategory> pageItems = filteredCategories.subList(fromIndex, toIndex);
        categoryTable.getItems().clear();
        categoryTable.getItems().addAll(pageItems);
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

        Button nextBtn = new Button("Suivant Â»");
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
     * Export des catÃ©gories en PDF
     */
    @FXML
    private void handleExportPDF() {
        try {
            // Ouvrir un FileChooser pour choisir l'emplacement
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");

            // Nom du fichier avec date et heure
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            fileChooser.setInitialFileName("categories_" + timestamp + ".pdf");

            // Filtrer uniquement les PDF
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
            );

            // Afficher le dialogue
            File file = fileChooser.showSaveDialog(categoryTable.getScene().getWindow());

            if (file != null) {
                // CrÃ©er le PDF
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                // En-tÃªte
                Paragraph header = new Paragraph("EventFlow - Liste des CatÃ©gories")
                        .setFontSize(20)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(13, 71, 161));
                document.add(header);

                // Date d'exportation
                String dateExport = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy Ã  HH:mm", Locale.FRENCH)
                );
                Paragraph dateInfo = new Paragraph("ExportÃ© le " + dateExport)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.GRAY);
                document.add(dateInfo);

                // Espacement
                document.add(new Paragraph("\n"));

                // Statistiques
                int totalCat = allCategories.size();
                int activeCat = (int) allCategories.stream().filter(EventCategory::isActive).count();
                int inactiveCat = totalCat - activeCat;
                int totalEvents = allCategories.stream().mapToInt(EventCategory::getEventCount).sum();

                Paragraph stats = new Paragraph(
                        String.format("Total: %d catÃ©gories | Actives: %d | Inactives: %d | Ã‰vÃ©nements: %d",
                                totalCat, activeCat, inactiveCat, totalEvents))
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setItalic();
                document.add(stats);

                document.add(new Paragraph("\n"));

                // CrÃ©er le tableau
                float[] columnWidths = {3, 5, 2, 2, 2};
                Table table = new Table(UnitValue.createPercentArray(columnWidths))
                        .useAllAvailableWidth();

                // En-tÃªtes de colonnes
                String[] headers = {"CatÃ©gorie", "Description", "Couleur", "Statut", "Ã‰vÃ©nements"};
                for (String headerText : headers) {
                    Cell headerCell = new Cell()
                            .add(new Paragraph(headerText).setBold())
                            .setBackgroundColor(new DeviceRgb(33, 150, 243))
                            .setFontColor(ColorConstants.WHITE)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(8);
                    table.addHeaderCell(headerCell);
                }

                // Ajouter les donnÃ©es
                for (EventCategory category : allCategories) {
                    // Nom
                    table.addCell(new Cell()
                            .add(new Paragraph(category.getName()))
                            .setPadding(6));

                    // Description
                    String desc = category.getDescription();
                    if (desc == null || desc.isEmpty()) desc = "-";
                    if (desc.length() > 80) desc = desc.substring(0, 77) + "...";
                    table.addCell(new Cell()
                            .add(new Paragraph(desc))
                            .setPadding(6));

                    // Couleur
                    String colorName = getColorName(category.getColor());
                    table.addCell(new Cell()
                            .add(new Paragraph(colorName))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));

                    // Statut
                    String status = category.isActive() ? "âœ“ Actif" : "âœ— Inactif";
                    DeviceRgb statusColor = category.isActive()
                            ? new DeviceRgb(76, 175, 80)
                            : new DeviceRgb(244, 67, 54);
                    table.addCell(new Cell()
                            .add(new Paragraph(status))
                            .setFontColor(statusColor)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));

                    // Nombre d'Ã©vÃ©nements
                    table.addCell(new Cell()
                            .add(new Paragraph(String.valueOf(category.getEventCount())))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(6));
                }

                document.add(table);

                // Footer
                document.add(new Paragraph("\n"));
                Paragraph footer = new Paragraph("Â© 2026 EventFlow - Gestion des Ã©vÃ©nements")
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.LIGHT_GRAY);
                document.add(footer);

                // Fermer le document
                document.close();

                // Afficher un message de succÃ¨s
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export rÃ©ussi");
                alert.setHeaderText(null);
                alert.setContentText("Le PDF a Ã©tÃ© exportÃ© avec succÃ¨s !\n\n" +
                        totalCat + " catÃ©gories exportÃ©es.\nFichier: " + file.getName());
                alert.showAndWait();

                System.out.println("âœ… PDF exportÃ©: " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("âŒ Erreur export PDF: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'export");
            alert.setHeaderText("Impossible d'exporter le PDF");
            alert.setContentText("Erreur: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Convertit un code couleur hex en nom lisible
     */
    private String getColorName(String hex) {
        if (hex == null) return "Aucune";
        switch (hex.toLowerCase()) {
            case "#ff5733": return "Rouge";
            case "#33ff57": return "Vert";
            case "#3357ff": return "Bleu";
            case "#ffff33": return "Jaune";
            case "#ff33ff": return "Magenta";
            case "#33ffff": return "Cyan";
            case "#ff8c00": return "Orange";
            case "#9932cc": return "Violet";
            default: return hex;
        }
    }
}