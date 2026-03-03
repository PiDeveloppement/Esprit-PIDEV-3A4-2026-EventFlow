package com.example.pidev.controller.role;

import com.example.pidev.MainController;
import com.example.pidev.model.role.Role;
import com.example.pidev.service.role.RoleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class RoleController implements Initializable {

    // @FXML private Label totalRolesLabel; // SUPPRIMÉ - DÉPLACÉ DANS LE HEADER
    @FXML private TextField searchField;
    @FXML private TableView<Role> roleTable;
    @FXML private TableColumn<Role, String> roleNameColumn;
    @FXML private TableColumn<Role, Void> actionsColumn;
    @FXML private Label paginationLabel;
    @FXML private Label statsLabel; // AJOUTÉ (optionnel)
    @FXML private Button prevPageBtn, nextPageBtn, page1Btn, page2Btn, page3Btn, lastPageBtn;

    private RoleService roleService;
    private ObservableList<Role> rolesList;
    private FilteredList<Role> filteredData;
    private MainController mainController;

    private int currentPage = 1;
    private final int rowsPerPage = 5;
    private int totalPages = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            roleService = new RoleService();
            rolesList = FXCollections.observableArrayList();

            // 1. Initialiser les colonnes avec alignement vertical
            roleNameColumn.setCellValueFactory(new PropertyValueFactory<>("roleName"));
            roleNameColumn.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 20 0 20;");

            // 2. Fixer la taille des cellules pour éviter les lettres coupées
            roleTable.setFixedCellSize(55.0);

            // Ajuster la hauteur de la table pour 5 lignes + header
            double tableHeight = (5 * 55.0) + 40;
            roleTable.setPrefHeight(tableHeight);
            roleTable.setMinHeight(tableHeight);
            roleTable.setMaxHeight(tableHeight);

            roleTable.setPlaceholder(new Label("Aucun rôle à afficher"));

            loadRoles();
            setupActionsColumn();
            setupSearch();
            setupPaginationControls();

        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void loadRoles() {
        rolesList.setAll(roleService.getAllRoles());
        filteredData = new FilteredList<>(rolesList, p -> true);

        // Mettre à jour le KPI dans le header via MainController
        if (mainController != null) {
            mainController.refreshKPIs();
        }

        // Mettre à jour le label stats optionnel
        if (statsLabel != null) {
            statsLabel.setText(rolesList.size() + " rôles");
        }

        currentPage = 1;
        updateTableWithPagination();
    }

    private void updateTableWithPagination() {
        if (filteredData == null || filteredData.isEmpty()) {
            roleTable.setItems(FXCollections.observableArrayList());
            if (paginationLabel != null) paginationLabel.setText("Page 0 sur 0");
            return;
        }

        int totalItems = filteredData.size();
        totalPages = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (totalPages <= 0) totalPages = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        roleTable.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        if (paginationLabel != null) paginationLabel.setText("Page " + currentPage + " sur " + totalPages);
        updatePaginationButtons();
    }

    private void setupPaginationControls() {
        page1Btn.setOnAction(e -> goToPage(1));
        page2Btn.setOnAction(e -> goToPage(2));
        page3Btn.setOnAction(e -> goToPage(3));
        prevPageBtn.setOnAction(e -> goToPage(currentPage - 1));
        nextPageBtn.setOnAction(e -> goToPage(currentPage + 1));
        if (lastPageBtn != null) lastPageBtn.setOnAction(e -> goToPage(totalPages));
    }

    private void goToPage(int page) {
        if (page < 1 || page > totalPages) return;
        currentPage = page;
        updateTableWithPagination();
    }

    private void updatePaginationButtons() {
        page1Btn.setStyle(getPageButtonStyle(1));
        page2Btn.setStyle(getPageButtonStyle(2));
        page3Btn.setStyle(getPageButtonStyle(3));
        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage == totalPages);

        page1Btn.setVisible(totalPages >= 1);
        page2Btn.setVisible(totalPages >= 2);
        page3Btn.setVisible(totalPages >= 3);
    }

    private String getPageButtonStyle(int page) {
        return (page == currentPage)
                ? "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 8;"
                : "-fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #e2e8f0; -fx-background-radius: 8;";
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("📝");
            private final Button deleteBtn = new Button("✂");
            private final HBox container = new HBox(12, editBtn, deleteBtn);
            {
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 5 10;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 5 10;");
                editBtn.setOnAction(e -> mainController.loadEditRolePage(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> {
                    Role r = getTableView().getItems().get(getIndex());
                    if (showConfirmation("Suppression", "Supprimer le rôle " + r.getRoleName() + " ?")) {
                        if (roleService.deleteRole(r.getId_Role())) {
                            loadRoles(); // Recharger après suppression
                        }
                    }
                });
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newValue) -> {
            filteredData.setPredicate(role -> newValue.isEmpty() || role.getRoleName().toLowerCase().contains(newValue.toLowerCase()));
            currentPage = 1;
            updateTableWithPagination();
        });
    }

    // Méthode supprimée - déplacée dans MainController
    // private void updateTotalRolesCount() { ... }

    @FXML private void addRoleButtonOnAction(ActionEvent e) {
        if (mainController != null) mainController.loadAddRolePage();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // Méthode pour recharger les données (utile après ajout/modification)
    public void refreshData() {
        loadRoles();
    }

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private boolean showConfirmation(String title, String msg) {
        return new Alert(Alert.AlertType.CONFIRMATION, msg).showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}