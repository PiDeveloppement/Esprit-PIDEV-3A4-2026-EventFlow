package com.example.pidev.controller.user;

import com.example.pidev.MainController;
import com.example.pidev.model.role.Role;
import com.example.pidev.model.user.PasswordResetToken;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.role.RoleService;
import com.example.pidev.service.user.PasswordResetService;
import com.example.pidev.service.user.UserService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class UserController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<UserModel> userTable;
    @FXML private TableColumn<UserModel, String> firstname_column;
    @FXML private TableColumn<UserModel, String> lastname_column;
    @FXML private TableColumn<UserModel, String> email_column;
    @FXML private TableColumn<UserModel, String> faculte_column;
    @FXML private TableColumn<UserModel, String> password_column;
    @FXML private TableColumn<UserModel, String> role_column;
    @FXML private TableColumn<UserModel, Void> actions_column;

    @FXML private ComboBox<String> faculteFilterCombo;
    @FXML private ComboBox<String> roleFilterCombo;
    // @FXML private Label totalParticipantsLabel; // COMMENTÉ CAR DÉPLACÉ DANS LE HEADER

    @FXML private Button prevPageBtn, nextPageBtn, page1Btn, page2Btn, page3Btn, lastPageBtn;
    @FXML private Label paginationLabel, statsLabel;

    private MainController mainController;
    private UserService userService;
    private RoleService roleService;
    private ObservableList<UserModel> usersList;
    private FilteredList<UserModel> filteredData;

    private int currentPage = 1;
    private final int rowsPerPage = 5;
    private int totalPages = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            userService = new UserService();
            roleService = new RoleService();
            usersList = FXCollections.observableArrayList();

            initializeTableColumns();

            // FIX: Augmentation de la taille de cellule pour la visibilité des lettres
            userTable.setFixedCellSize(55.0);

            // Empêcher la table d'être trop petite
            userTable.setMinHeight(350);

            // Chargement initial des données
            loadUsers();

            // Configuration des contrôles
            loadFaculteFilterList();
            loadRoleFilterList();
            setupSearch();
            setupActionsColumn();
            setupPaginationControls();

            // La mise à jour du KPI est maintenant gérée par MainController
            // via showParticipantKPIs()

        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void initializeTableColumns() {
        firstname_column.setCellValueFactory(new PropertyValueFactory<>("first_Name"));
        lastname_column.setCellValueFactory(new PropertyValueFactory<>("last_Name"));
        email_column.setCellValueFactory(new PropertyValueFactory<>("email"));
        faculte_column.setCellValueFactory(new PropertyValueFactory<>("faculte"));
        password_column.setCellValueFactory(new PropertyValueFactory<>("password"));

        role_column.setCellValueFactory(cell -> {
            Role r = cell.getValue().getRole();
            return new SimpleStringProperty(r != null ? r.getRoleName() : "");
        });

        // Appliquer un alignement centré verticalement à toutes les colonnes
        userTable.getColumns().forEach(column -> {
            column.setStyle("-fx-alignment: CENTER-LEFT;");
        });
    }

    private void loadUsers() {
        try {
            System.out.println("Chargement automatique des utilisateurs...");
            usersList.setAll(userService.getAllUsers());
            System.out.println("Nombre d'utilisateurs chargés: " + usersList.size());

            filteredData = new FilteredList<>(usersList, p -> true);
            currentPage = 1;
            updateTableWithPagination();

            // Mettre à jour le KPI dans le header via MainController
            if (mainController != null) {
                mainController.refreshKPIs();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les utilisateurs: " + e.getMessage());
        }
    }

    private void updateTableWithPagination() {
        if (filteredData == null) return;

        int totalItems = filteredData.size();
        totalPages = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (totalPages <= 0) totalPages = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        if (fromIndex < totalItems) {
            userTable.setItems(FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex)));
        } else {
            userTable.setItems(FXCollections.observableArrayList());
        }

        if (paginationLabel != null) paginationLabel.setText("Page " + currentPage + " sur " + totalPages);
        if (statsLabel != null) statsLabel.setText(totalItems + " utilisateurs");

        updatePaginationButtons();
    }

    private void setupPaginationControls() {
        page1Btn.setOnAction(e -> goToPage(1));
        page2Btn.setOnAction(e -> goToPage(2));
        page3Btn.setOnAction(e -> goToPage(3));
        prevPageBtn.setOnAction(e -> goToPage(currentPage - 1));
        nextPageBtn.setOnAction(e -> goToPage(currentPage + 1));
        lastPageBtn.setOnAction(e -> goToPage(totalPages));
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

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, newValue) -> applyFilters());
    }

    private void applyFilters() {
        if (filteredData == null) return;

        String selectedFaculte = faculteFilterCombo.getValue();
        String selectedRole = roleFilterCombo.getValue();
        String keyword = searchField.getText().toLowerCase().trim();

        filteredData.setPredicate(user -> {
            boolean matchesFaculte = (selectedFaculte == null || selectedFaculte.isEmpty() || "Toutes les facultés".equals(selectedFaculte)) ||
                    (user.getFaculte() != null && user.getFaculte().equalsIgnoreCase(selectedFaculte));
            boolean matchesRole = (selectedRole == null || selectedRole.isEmpty() || "Tous les rôles".equals(selectedRole)) ||
                    (user.getRole() != null && user.getRole().getRoleName().equalsIgnoreCase(selectedRole));
            boolean matchesSearch = keyword.isEmpty() ||
                    (user.getFirst_Name() != null && user.getFirst_Name().toLowerCase().contains(keyword)) ||
                    (user.getLast_Name() != null && user.getLast_Name().toLowerCase().contains(keyword)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(keyword));

            return matchesFaculte && matchesRole && matchesSearch;
        });
        currentPage = 1;
        updateTableWithPagination();
    }

    private void setupActionsColumn() {
        actions_column.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("📝");
            private final Button deleteBtn = new Button("✂");
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                editBtn.setOnAction(e -> openEditPage(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> confirmAndDelete(getTableView().getItems().get(getIndex())));
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void confirmAndDelete(UserModel user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + user.getFirst_Name() + " " + user.getLast_Name() + " ?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (userService.deleteUser(user.getId_User())) {
                    loadUsers(); // Recharger les données après suppression
                    showInfo("Succès", "Utilisateur supprimé avec succès");
                }
            }
        });
    }

    private void openEditPage(UserModel user) {
        if (mainController != null) mainController.loadEditUserPage(user);
    }

    private void loadFaculteFilterList() {
        try {
            ObservableList<String> facultes = FXCollections.observableArrayList(userService.getAllFacultes());
            facultes.add(0, "Toutes les facultés");
            faculteFilterCombo.setItems(facultes);
            faculteFilterCombo.getSelectionModel().select(0);
        } catch (Exception ignored) {}
    }

    private void loadRoleFilterList() {
        try {
            ObservableList<String> roles = FXCollections.observableArrayList(roleService.getAllRoleNames());
            roles.add(0, "Tous les rôles");
            roleFilterCombo.setItems(roles);
            roleFilterCombo.getSelectionModel().select(0);
        } catch (Exception ignored) {}
    }

    @FXML private void filterByFaculte() { applyFilters(); }
    @FXML private void filterByRole() { applyFilters(); }

    @FXML private void resetFilters() {
        faculteFilterCombo.getSelectionModel().select(0);
        roleFilterCombo.getSelectionModel().select(0);
        searchField.clear();
        applyFilters();
    }

    // Méthode supprimée car le KPI est maintenant dans le header
    // private void updateTotalParticipantsCount() { ... }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // Méthode publique pour recharger les données (utile après ajout/modification)
    public void refreshData() {
        loadUsers();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setTitle(title);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setTitle(title);
        a.showAndWait();
    }
}