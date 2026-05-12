package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.resource.Salle;
import com.example.pidev.service.resource.SalleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SalleController implements Initializable {

    @FXML private TableView<Salle> salleTable;
    @FXML private TableColumn<Salle, String> imageCol, nameCol, buildingCol, statusCol;
    @FXML private TableColumn<Salle, Integer> capacityCol, floorCol;
    @FXML private TableColumn<Salle, Void> actionCol;

    @FXML private Label totalSallesLabel, sallesOccupeesLabel;
    @FXML private WebView mapWebView;

    @FXML private TextField searchNameField, searchCapMinField, searchCapMaxField;
    @FXML private ComboBox<String> filterBuildingCombo;
    @FXML private ComboBox<String> filterStatusCombo;

    private final SalleService service = new SalleService();
    private ObservableList<Salle> masterData = FXCollections.observableArrayList();
    // Ajout de la FilteredList pour la recherche
    private FilteredList<Salle> filteredData;
    private WebEngine webEngine;

    private static final double LAT_ESPRIT = 36.8993;
    private static final double LON_ESPRIT = 10.1887;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        webEngine = mapWebView.getEngine();

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("building"));
        floorCol.setCellValueFactory(new PropertyValueFactory<>("floor"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        imageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        imageCol.setCellFactory(param -> new TableCell<>() {
            private final ImageView view = new ImageView();
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        view.setImage(new Image(path, 40, 40, true, true));
                        setGraphic(view);
                    } catch (Exception e) { setGraphic(null); }
                }
            }
        });

        salleTable.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> {
            if (s != null) updateMapForBloc(s.getBuilding(), s.getName());
        });

        setupActionColumn();

        // Initialisation des ComboBox
        if(filterStatusCombo != null) {
            filterStatusCombo.getItems().addAll("Tous", "DISPONIBLE", "OCCUPEE");
            filterStatusCombo.setValue("Tous");
        }
        if(filterBuildingCombo != null) {
            filterBuildingCombo.getItems().addAll("Tous", "A", "B", "C", "G", "I", "J", "K", "M");
            filterBuildingCombo.setValue("Tous");
        }

        loadTable();

        // Configuration de la recherche dynamique
        setupSearchLogic();

        updateMap(LAT_ESPRIT, LON_ESPRIT, "Campus Esprit");
    }

    private void setupSearchLogic() {
        // Écouteurs sur les champs de texte
        searchNameField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchCapMinField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchCapMaxField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Écouteurs sur les ComboBox
        filterBuildingCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(salle -> {
            // Filtre par Nom
            String nameSearch = searchNameField.getText().toLowerCase();
            if (!nameSearch.isEmpty() && !salle.getName().toLowerCase().contains(nameSearch)) return false;

            // Filtre par Bâtiment
            String buildingFilter = filterBuildingCombo.getValue();
            if (buildingFilter != null && !buildingFilter.equals("Tous") && !salle.getBuilding().equalsIgnoreCase(buildingFilter)) return false;

            // Filtre par Statut
            String statusFilter = filterStatusCombo.getValue();
            if (statusFilter != null && !statusFilter.equals("Tous") && !salle.getStatus().equalsIgnoreCase(statusFilter)) return false;

            // Filtre par Capacité (Min/Max)
            try {
                if (!searchCapMinField.getText().isEmpty()) {
                    int min = Integer.parseInt(searchCapMinField.getText());
                    if (salle.getCapacity() < min) return false;
                }
                if (!searchCapMaxField.getText().isEmpty()) {
                    int max = Integer.parseInt(searchCapMaxField.getText());
                    if (salle.getCapacity() > max) return false;
                }
            } catch (NumberFormatException e) {
                // Ignore l'erreur si l'utilisateur tape autre chose que des chiffres
            }

            return true;
        });
        updateStats();
    }

    private void loadTable() {
        masterData.setAll(service.afficher());

        // 1. Initialiser la FilteredList
        filteredData = new FilteredList<>(masterData, p -> true);

        // 2. Envelopper dans une SortedList pour permettre le tri
        SortedList<Salle> sortedData = new SortedList<>(filteredData);

        // 3. Lier le comparateur de la SortedList à celui de la TableView
        sortedData.comparatorProperty().bind(salleTable.comparatorProperty());

        // 4. Définir les données dans la table
        salleTable.setItems(sortedData);

        updateStats();
    }

    // ================= NAVIGATION VERS FORMULAIRE =================

    @FXML
    private void ouvrirPopupAjout() {
        allerVersFormulaire(null);
    }

    private void allerVersFormulaire(Salle salle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/resource/form_salle.fxml"));
            Parent root = loader.load();

            SalleFormController controller = loader.getController();
            if (salle != null) {
                controller.setSalleData(salle);
            }

            MainController.getInstance().setContent(root);
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur de chargement du formulaire").show();
        }
    }

    @FXML
    private void exportPDF(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export PDF");
        alert.setHeaderText(null);
        alert.setContentText("La génération du PDF a été lancée pour " + filteredData.size() + " salles.");
        alert.showAndWait();
    }

    @FXML
    private void resetFilters(ActionEvent event) {
        if (searchNameField != null) searchNameField.clear();
        if (searchCapMinField != null) searchCapMinField.clear();
        if (searchCapMaxField != null) searchCapMaxField.clear();
        if (filterBuildingCombo != null) filterBuildingCombo.setValue("Tous");
        if (filterStatusCombo != null) filterStatusCombo.setValue("Tous");
        applyFilters();
    }

    private void updateStats() {
        totalSallesLabel.setText(String.valueOf(filteredData.size()));
        long occ = filteredData.stream().filter(s -> "OCCUPEE".equalsIgnoreCase(s.getStatus())).count();
        sallesOccupeesLabel.setText(String.valueOf(occ));
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(p -> new TableCell<>() {
            private final Button edit = new Button("✎");
            private final Button del = new Button("🗑");
            private final HBox box = new HBox(8, edit, del);
            {
                edit.setStyle("-fx-cursor: hand;");
                del.setStyle("-fx-cursor: hand;");
                edit.setOnAction(e -> allerVersFormulaire(getTableView().getItems().get(getIndex())));
                del.setOnAction(e -> {
                    Salle s = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la salle " + s.getName() + " ?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            service.supprimer(s.getId());
                            loadTable();
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void i, boolean empty) {
                super.updateItem(i, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void updateMapForBloc(String bloc, String salleNom) {
        double lat, lon;
        if (bloc == null) bloc = "A";
        bloc = bloc.toUpperCase().trim();
        switch (bloc) {
            case "A": case "B": case "C": lat = 36.898778; lon = 10.188694; break;
            case "G": lat = 36.8985482; lon = 10.1887448; break;
            case "I": case "J": case "K": lat = 36.9010594; lon = 10.190243; break;
            case "M": lat = 36.9021262; lon = 10.1893184; break;
            default: lat = LAT_ESPRIT; lon = LON_ESPRIT;
        }
        updateMap(lat, lon, "Bloc " + bloc + " - " + salleNom);
    }

    private void updateMap(double lat, double lon, String title) {
        String html = "<html><head><link rel='stylesheet' href='https://unpkg.com/leaflet@1.7.1/dist/leaflet.css'/><script src='https://unpkg.com/leaflet@1.7.1/dist/leaflet.js'></script><style>#map {height:100%;width:100%;margin:0;}</style></head><body><div id='map'></div><script>var map=L.map('map').setView(["+lat+","+lon+"],18);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);L.marker(["+lat+","+lon+"]).addTo(map).bindPopup('"+title+"').openPopup();</script></body></html>";
        webEngine.loadContent(html);
    }
}