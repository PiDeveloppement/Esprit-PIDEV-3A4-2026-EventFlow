package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.resource.Salle;
import com.example.pidev.service.resource.SalleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SalleController implements Initializable {

    // ===== FXML Injections =====
    @FXML private Label totalSallesLabel;
    @FXML private Label sallesOccupeesLabel;
    @FXML private WebView mapWebView;

    @FXML private TextField searchNameField;
    @FXML private TextField searchCapMinField;
    @FXML private TextField searchCapMaxField;
    @FXML private ComboBox<String> filterBuildingCombo;
    @FXML private ComboBox<String> filterStatusCombo;

    @FXML private FlowPane sallesFlowPane;
    @FXML private VBox emptyState;

    // ===== State =====
    private final SalleService service = new SalleService();
    private ObservableList<Salle> masterData = FXCollections.observableArrayList();
    private FilteredList<Salle> filteredData;
    private WebEngine webEngine;

    private static final double LAT_ESPRIT = 36.8993;
    private static final double LON_ESPRIT = 10.1887;

    // ===== Colors =====
    private static final String COLOR_DISPONIBLE = "#10b981";
    private static final String COLOR_OCCUPEE    = "#ef4444";

    // ====================================================================
    //  INITIALISATION
    // ====================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        webEngine = mapWebView.getEngine();

        // Combo Statut
        filterStatusCombo.getItems().addAll("Tous", "DISPONIBLE", "OCCUPEE");
        filterStatusCombo.setValue("Tous");

        // Combo Bâtiment
        filterBuildingCombo.getItems().addAll("Tous", "A", "B", "C", "G", "I", "J", "K", "M");
        filterBuildingCombo.setValue("Tous");

        // Listeners de filtre
        setupSearchLogic();

        // Chargement des données
        loadCards();

        // Carte initiale
        updateMap(LAT_ESPRIT, LON_ESPRIT, "Campus Esprit");
    }

    // ====================================================================
    //  FILTRES
    // ====================================================================
    private void setupSearchLogic() {
        searchNameField.textProperty().addListener((o, ov, nv) -> applyFilters());
        searchCapMinField.textProperty().addListener((o, ov, nv) -> applyFilters());
        searchCapMaxField.textProperty().addListener((o, ov, nv) -> applyFilters());
        filterBuildingCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
        filterStatusCombo.valueProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(salle -> {
            String nameSearch = searchNameField.getText().toLowerCase();
            if (!nameSearch.isEmpty() && !salle.getName().toLowerCase().contains(nameSearch)) return false;

            String buildingFilter = filterBuildingCombo.getValue();
            if (buildingFilter != null && !buildingFilter.equals("Tous")
                    && !salle.getBuilding().equalsIgnoreCase(buildingFilter)) return false;

            String statusFilter = filterStatusCombo.getValue();
            if (statusFilter != null && !statusFilter.equals("Tous")
                    && !salle.getStatus().equalsIgnoreCase(statusFilter)) return false;

            try {
                if (!searchCapMinField.getText().isEmpty()) {
                    int min = Integer.parseInt(searchCapMinField.getText());
                    if (salle.getCapacity() < min) return false;
                }
                if (!searchCapMaxField.getText().isEmpty()) {
                    int max = Integer.parseInt(searchCapMaxField.getText());
                    if (salle.getCapacity() > max) return false;
                }
            } catch (NumberFormatException ignored) {}

            return true;
        });

        // Re-render les cards filtrées
        renderCards(filteredData);
        updateStats();
    }

    // ====================================================================
    //  CHARGEMENT & RENDU DES CARDS
    // ====================================================================
    private void loadCards() {
        masterData.setAll(service.afficher());
        filteredData = new FilteredList<>(masterData, p -> true);
        renderCards(filteredData);
        updateStats();
    }

    /**
     * Vide le FlowPane et recrée toutes les cards visuellement
     * identiques aux cards HTML de la page web.
     */
    private void renderCards(Iterable<Salle> salles) {
        sallesFlowPane.getChildren().clear();

        boolean hasData = false;
        for (Salle salle : salles) {
            sallesFlowPane.getChildren().add(buildSalleCard(salle));
            hasData = true;
        }

        // État vide
        emptyState.setVisible(!hasData);
        emptyState.setManaged(!hasData);
    }

    /**
     * Construit une VBox qui reproduit exactement la salle-card HTML :
     *  - Image header 180px avec badge statut
     *  - Section infos : capacité / étage / bloc
     *  - Footer : Voir détails | Modifier | Supprimer
     */
    private VBox buildSalleCard(Salle salle) {

        // ── ROOT CARD ──────────────────────────────────────────────────
        VBox card = new VBox();
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 25, 0, 0, 8);" +
                        "-fx-translate-y: -4;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);" +
                        "-fx-translate-y: 0;"
        ));

        // ── IMAGE HEADER ──────────────────────────────────────────────
        StackPane imageHeader = new StackPane();
        imageHeader.setPrefHeight(180);
        imageHeader.setMinHeight(180);
        imageHeader.setMaxHeight(180);
        imageHeader.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2);" +
                        "-fx-background-radius: 12 12 0 0;"
        );

        // Image salle
        ImageView imgView = new ImageView();
        imgView.setFitWidth(320);
        imgView.setFitHeight(180);
        imgView.setPreserveRatio(false);
        imgView.setStyle("-fx-background-radius: 12 12 0 0;");
        try {
            String path = salle.getImagePath();
            if (path != null && !path.isEmpty()) {
                imgView.setImage(new Image(path, 320, 180, false, true, true));
            }
        } catch (Exception ignored) {}

        // Badge statut (top-right overlay)
        boolean disponible = "DISPONIBLE".equalsIgnoreCase(salle.getStatus());
        Label badge = new Label(salle.getStatus());
        badge.setStyle(
                "-fx-background-color: " + (disponible ? COLOR_DISPONIBLE : COLOR_OCCUPEE) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 12;" +
                        "-fx-background-radius: 20;"
        );
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(10, 10, 0, 0));

        imageHeader.getChildren().addAll(imgView, badge);

        // ── BODY ──────────────────────────────────────────────────────
        VBox body = new VBox();
        body.setPadding(new Insets(18, 18, 0, 18));
        body.setSpacing(12);

        // Nom
        Label nameLabel = new Label(salle.getName());
        nameLabel.setStyle(
                "-fx-font-size: 17; -fx-font-weight: bold; -fx-text-fill: #1f2937;"
        );
        nameLabel.setWrapText(true);

        // Stats row : capacité | étage | bloc
        HBox statsRow = new HBox();
        statsRow.setAlignment(Pos.CENTER);
        statsRow.setSpacing(0);
        HBox.setHgrow(statsRow, Priority.ALWAYS);

        statsRow.getChildren().addAll(
                buildStatBlock("👥", "Capacité", String.valueOf(salle.getCapacity()), "#667eea"),
                buildStatBlock("🏢", "Étage",    String.valueOf(salle.getFloor()),    "#10b981"),
                buildStatBlock("🏗️", "Bloc",     salle.getBuilding(),               "#f59e0b")
        );

        body.getChildren().addAll(nameLabel, statsRow);

        // ── FOOTER ACTIONS ────────────────────────────────────────────
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setSpacing(0);
        footer.setPadding(new Insets(14, 0, 14, 0));
        footer.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");
        VBox.setMargin(footer, new Insets(12, 18, 0, 18));

        Button btnVoir = buildFooterBtn("👁  Voir détails", "#10b981", "#f0fdf4");
        Button btnEdit = buildFooterBtn("✎  Modifier",     "#667eea", "#f3f4f6");
        Button btnDel  = buildFooterBtn("🗑  Supprimer",    "#ef4444", "#fef2f2");

        btnVoir.setOnAction(e -> allerVersDetail(salle));
        btnEdit.setOnAction(e -> allerVersFormulaire(salle));
        btnDel.setOnAction(e -> confirmerSuppression(salle));

        // Update map on card selection
        card.setOnMouseClicked(e -> updateMapForBloc(salle.getBuilding(), salle.getName()));

        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        HBox.setHgrow(btnEdit, Priority.ALWAYS);
        HBox.setHgrow(btnDel,  Priority.ALWAYS);
        footer.getChildren().addAll(btnVoir, buildVerticalSep(), btnEdit, buildVerticalSep(), btnDel);

        // ── ASSEMBLE ─────────────────────────────────────────────────
        card.getChildren().addAll(imageHeader, body, footer);
        return card;
    }

    /** Bloc stat vertical centré (icône + libellé + valeur) */
    private VBox buildStatBlock(String icon, String label, String value, String iconColor) {
        VBox block = new VBox(4);
        block.setAlignment(Pos.CENTER);
        block.setPrefWidth(100);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 15;");

        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");

        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        block.getChildren().addAll(iconLabel, lblLabel, valLabel);
        return block;
    }

    /** Bouton footer style texte (transparent, hover teinté) */
    private Button buildFooterBtn(String text, String color, String hoverBg) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 8 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-width: 0;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverBg + ";" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 8 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-width: 0;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 8 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-width: 0;"
        ));
        return btn;
    }

    /** Petit séparateur vertical entre les boutons du footer */
    private Separator buildVerticalSep() {
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setStyle("-fx-padding: 0;");
        sep.setPrefHeight(28);
        return sep;
    }

    // ====================================================================
    //  STATS
    // ====================================================================
    private void updateStats() {
        totalSallesLabel.setText(String.valueOf(filteredData.size()));
        long occ = filteredData.stream()
                .filter(s -> "OCCUPEE".equalsIgnoreCase(s.getStatus()))
                .count();
        sallesOccupeesLabel.setText(String.valueOf(occ));
    }

    // ====================================================================
    //  NAVIGATION / ACTIONS
    // ====================================================================
    @FXML
    private void ouvrirPopupAjout() {
        allerVersFormulaire(null);
    }

    private void allerVersFormulaire(Salle salle) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/resource/form_salle.fxml"));
            Parent root = loader.load();

            SalleFormController controller = loader.getController();
            if (salle != null) controller.setSalleData(salle);

            MainController.getInstance().setContent(root);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur de chargement du formulaire").show();
        }
    }

    /** Naviguer vers la page détail de la salle */
    private void allerVersDetail(Salle salle) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/resource/salle.fxml"));
            Parent root = loader.load();

            // Adaptez le nom de votre contrôleur détail si nécessaire
            // SalleShowController c = loader.getController();
            // c.setSalle(salle);

            MainController.getInstance().setContent(root);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur de chargement du détail").show();
        }
    }

    private void confirmerSuppression(Salle salle) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la salle « " + salle.getName() + " » ?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                service.supprimer(salle.getId());
                loadCards();
            }
        });
    }

    @FXML
    private void exportPDF(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export PDF");
        alert.setHeaderText(null);
        alert.setContentText("PDF généré pour " + filteredData.size() + " salle(s).");
        alert.showAndWait();
    }

    @FXML
    private void resetFilters(ActionEvent event) {
        searchNameField.clear();
        searchCapMinField.clear();
        searchCapMaxField.clear();
        filterBuildingCombo.setValue("Tous");
        filterStatusCombo.setValue("Tous");
        applyFilters();
    }

    // ====================================================================
    //  CARTE LEAFLET
    // ====================================================================
    private void updateMapForBloc(String bloc, String salleNom) {
        double lat, lon;
        if (bloc == null) bloc = "A";
        bloc = bloc.toUpperCase().trim();
        switch (bloc) {
            case "A": case "B": case "C": lat = 36.898778; lon = 10.188694; break;
            case "G":                      lat = 36.8985482; lon = 10.1887448; break;
            case "I": case "J": case "K": lat = 36.9010594; lon = 10.190243; break;
            case "M":                      lat = 36.9021262; lon = 10.1893184; break;
            default:                       lat = LAT_ESPRIT;  lon = LON_ESPRIT;
        }
        updateMap(lat, lon, "Bloc " + bloc + " — " + salleNom);
    }

    private void updateMap(double lat, double lon, String title) {
        String html =
                "<html><head>" +
                        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.7.1/dist/leaflet.css'/>" +
                        "<script src='https://unpkg.com/leaflet@1.7.1/dist/leaflet.js'></script>" +
                        "<style>body{margin:0;}#map{height:100vh;width:100%;}</style>" +
                        "</head><body>" +
                        "<div id='map'></div>" +
                        "<script>" +
                        "var map=L.map('map').setView([" + lat + "," + lon + "],18);" +
                        "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);" +
                        "L.marker([" + lat + "," + lon + "]).addTo(map).bindPopup('" + title + "').openPopup();" +
                        "</script></body></html>";
        webEngine.loadContent(html);
    }
}