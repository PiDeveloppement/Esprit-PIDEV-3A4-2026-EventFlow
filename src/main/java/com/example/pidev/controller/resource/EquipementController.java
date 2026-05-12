package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.resource.Equipement;
import com.example.pidev.service.resource.EquipementService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class EquipementController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label totalArticlesLabel, aReparerLabel;
    @FXML private TableView<Equipement> equipementTable;
    @FXML private TableColumn<Equipement, String> imageCol, nameCol, typeCol, statusCol;
    @FXML private TableColumn<Equipement, Integer> quantityCol;
    @FXML private TableColumn<Equipement, Void> actionCol;

    private final EquipementService service = new EquipementService();
    private ObservableList<Equipement> masterData = FXCollections.observableArrayList();
    private FilteredList<Equipement> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadTable();
        setupSearchLogic();
    }

    private void setupColumns() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        imageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        // STYLE IMAGE : Gestion robuste des chemins (Fix pour les erreurs "Fichier introuvable")
        imageCol.setCellFactory(param -> new TableCell<>() {
            private final ImageView view = new ImageView();

            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        // Nettoyage du chemin stocké en base
                        String cleanPath = path.replace("file:/", "").replace("%20", " ");
                        File file = new File(cleanPath);

                        // FALLBACK : Si le chemin complet src/uploads ne marche pas,
                        // on cherche dans le dossier uploads à la racine du projet
                        if (!file.exists()) {
                            String fileName = new File(path).getName();
                            file = new File("uploads/" + fileName);
                        }

                        if (file.exists()) {
                            Image img = new Image(file.toURI().toString(), 100, 100, true, true);
                            view.setImage(img);
                            view.setFitHeight(45);
                            view.setFitWidth(45);
                            Circle clip = new Circle(22.5, 22.5, 22.5);
                            view.setClip(clip);
                            setGraphic(view);
                        } else {
                            // Si l'image n'existe nulle part
                            setGraphic(new Label("🚫"));
                            System.out.println("❌ Image manquante : " + path);
                        }
                    } catch (Exception e) {
                        setGraphic(new Label("⚠️"));
                    }
                }
                setAlignment(Pos.CENTER);
            }
        });

        // STYLE STATUT : Badges couleurs
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label badge = new Label(item.toUpperCase());
                    String style = "-fx-padding: 5 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-text-fill: white;";
                    if (item.equalsIgnoreCase("EN_PANNE")) style += "-fx-background-color: #ef4444;";
                    else style += "-fx-background-color: #10b981;";
                    badge.setStyle(style);
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        setupActionColumn();
    }

    private void loadTable() {
        masterData.setAll(service.afficher());
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Equipement> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(equipementTable.comparatorProperty());
        equipementTable.setItems(sortedData);

        // Remplir les catégories
        ObservableList<String> categories = FXCollections.observableArrayList("Toutes les catégories");
        categories.addAll(masterData.stream().map(Equipement::getType).distinct().collect(Collectors.toList()));
        categoryFilter.setItems(categories);
        categoryFilter.getSelectionModel().selectFirst();

        updateStats();
    }

    private void setupSearchLogic() {
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(eq -> {
            String searchText = (searchField.getText() == null) ? "" : searchField.getText().toLowerCase();
            String selectedCat = categoryFilter.getValue();

            boolean matchesSearch = searchText.isEmpty() || eq.getName().toLowerCase().contains(searchText);
            boolean matchesCategory = selectedCat == null || selectedCat.equals("Toutes les catégories") || eq.getType().equals(selectedCat);

            return matchesSearch && matchesCategory;
        });
        updateStats();
    }

    private void updateStats() {
        totalArticlesLabel.setText(String.valueOf(filteredData.size()));
        long enPanne = filteredData.stream().filter(e -> "EN_PANNE".equalsIgnoreCase(e.getStatus())).count();
        aReparerLabel.setText(String.valueOf(enPanne));
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(12, editBtn, deleteBtn);
            {
                pane.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().add("action-button-update");
                deleteBtn.getStyleClass().add("action-button-delete");

                editBtn.setOnAction(e -> changerVersFormulaire(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> {
                    Equipement eq = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + eq.getName() + " ?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.YES) {
                            service.supprimer(eq.getId());
                            loadTable();
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML void ouvrirAjout() { changerVersFormulaire(null); }

    private void changerVersFormulaire(Equipement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/resource/form_equipement.fxml"));
            Parent root = loader.load();
            if (e != null) {
                EquipementFormController controller = loader.getController();
                controller.setEquipementData(e);
            }
            MainController.getInstance().setContent(root);
        } catch (IOException ex) { ex.printStackTrace(); }
    }
}