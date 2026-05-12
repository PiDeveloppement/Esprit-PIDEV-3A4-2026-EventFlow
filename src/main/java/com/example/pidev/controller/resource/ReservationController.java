package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.resource.ReservationResource;
import com.example.pidev.service.resource.ReservationService;
import com.example.pidev.utils.UserSession;

import com.itextpdf.layout.element.Cell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.kernel.colors.ColorConstants;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ReservationController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<ReservationResource> reservationTable;
    @FXML private TableColumn<ReservationResource, String> imgCol, nameCol, typeCol;
    @FXML private TableColumn<ReservationResource, Integer> qtyCol;
    @FXML private TableColumn<ReservationResource, LocalDateTime> startCol, endCol;
    @FXML private TableColumn<ReservationResource, Void> actionCol;
    @FXML private Label welcomeLabel;
    @FXML private Label userInfoLabel;
    @FXML private ComboBox<String> filterCombo;

    private final ObservableList<ReservationResource> masterData = FXCollections.observableArrayList();
    private final ReservationService resService = new ReservationService();
    private String userRole;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Récupérer les informations de session
        UserSession session = UserSession.getInstance();
        currentUserId = session.getUserId();
        userRole = session.getRole();

        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList("Plus récent", "Plus ancien"));
            sortCombo.setOnAction(e -> {
                String selection = sortCombo.getValue();
                if ("Plus récent".equals(selection)) {
                    trierParDate(false); // DESC
                } else if ("Plus ancien".equals(selection)) {
                    trierParDate(true);  // ASC
                }
            });
        }
        // Afficher les informations utilisateur
        if (session.isLoggedIn()) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Bienvenue, " + session.getFullName());
            }
            if (userInfoLabel != null) {
                userInfoLabel.setText("Connecté en tant que: " + userRole +
                        " (ID: " + currentUserId + ")");
            }
            System.out.println("📌 ReservationController - Utilisateur: " +
                    session.getFullName() + " (ID: " + currentUserId +
                    ", Rôle: " + userRole + ")");
        }

        setupTableColumns();
        setupSearchAndSort();
        setupFilterCombo();
        loadTable();
    }

    private void loadTable() {
        masterData.clear();

        // Charger les données selon le rôle
        if ("Admin".equals(userRole)) {
            // Les admins voient toutes les réservations
            masterData.addAll(resService.afficher());
            System.out.println("Admin: affichage de toutes les réservations");
        } else {
            // Les autres utilisateurs ne voient que leurs réservations
            // Note: Vous devrez ajouter cette méthode dans ReservationService
            // masterData.addAll(resService.getReservationsByUser(currentUserId));

            // Temporairement, on filtre après chargement
            masterData.addAll(resService.afficher());
            System.out.println("Utilisateur " + currentUserId +
                    ": affichage filtré par userId");
        }

        System.out.println("📋 " + masterData.size() + " réservations chargées");
    }

    private void setupTableColumns() {
        typeCol.setCellValueFactory(new PropertyValueFactory<>("resourceType"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("resourceName"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTimedate"));
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        imgCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        // Formatage des dates pour meilleure lisibilité
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        startCol.setCellFactory(column -> new TableCell<ReservationResource, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(formatter));
                }
            }
        });

        endCol.setCellFactory(column -> new TableCell<ReservationResource, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(formatter));
                }
            }
        });

        imgCol.setCellFactory(param -> new TableCell<>() {
            private final ImageView v = new ImageView();
            {
                v.setFitHeight(40);
                v.setFitWidth(40);
                v.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null) {
                    setGraphic(null);
                } else {
                    try {
                        String finalPath = path;
                        if (path.contains(":/") || path.contains(":\\")) {
                            if (!path.startsWith("file:")) {
                                finalPath = "file:/" + path.replace("\\", "/");
                            }
                        }
                        v.setImage(new Image(finalPath, 40, 40, true, true));
                        setGraphic(v);
                    } catch (Exception ex) {
                        setGraphic(null);
                    }
                }
            }
        });

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button edit = new Button("✎");
            private final Button del = new Button("🗑");
            private final HBox container = new HBox(10, edit, del);

            {
                // Style des boutons
                edit.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
                del.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");

                edit.setTooltip(new Tooltip("Modifier"));
                del.setTooltip(new Tooltip("Supprimer"));

                edit.setOnAction(e -> {
                    ReservationResource res = getTableView().getItems().get(getIndex());
                    goToForm(res);
                });

                del.setOnAction(e -> {
                    ReservationResource res = getTableView().getItems().get(getIndex());
                    handleDelete(res);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ReservationResource res = getTableView().getItems().get(getIndex());

                    // Vérifier les permissions
                    boolean canEdit = "Admin".equals(userRole) || res.getUserId() == currentUserId;

                    edit.setDisable(!canEdit);
                    del.setDisable(!canEdit);

                    // Tooltip personnalisé si pas de permission
                    if (!canEdit) {
                        edit.setTooltip(new Tooltip("Vous ne pouvez modifier que vos propres réservations"));
                        del.setTooltip(new Tooltip("Vous ne pouvez supprimer que vos propres réservations"));
                    }

                    setGraphic(container);
                }
            }
        });
    }

    private void setupFilterCombo() {
        if (filterCombo != null) {
            filterCombo.getItems().addAll("Toutes", "Mes réservations");
            filterCombo.setValue("Toutes");
            filterCombo.setOnAction(e -> applyFilter());
        }
    }
    private void trierParDate(boolean ascendant) {
        if (masterData == null || masterData.isEmpty()) return;

        if (ascendant) {
            // Tri ASC : a compareTo b
            masterData.sort((a, b) -> {
                if (a.getStartTimedate() == null || b.getStartTimedate() == null) return 0;
                return a.getStartTimedate().compareTo(b.getStartTimedate());
            });
        } else {
            // Tri DESC : b compareTo a
            masterData.sort((a, b) -> {
                if (a.getStartTimedate() == null || b.getStartTimedate() == null) return 0;
                return b.getStartTimedate().compareTo(a.getStartTimedate());
            });
        }

        // Optionnel : rafraîchir visuellement le tableau
        reservationTable.refresh();
    }

    private void applyFilter() {
        if (filterCombo == null) return;

        String filter = filterCombo.getValue();
        if ("Mes réservations".equals(filter)) {
            // Afficher seulement les réservations de l'utilisateur connecté
            masterData.clear();
            masterData.addAll(resService.afficher().stream()
                    .filter(r -> r.getUserId() == currentUserId)
                    .toList());
        } else {
            // Afficher toutes (ou selon le rôle)
            loadTable();
        }
    }

    @FXML
    void goToAdd() {
        goToForm(null);
    }

    @FXML
    private void goToForm(ReservationResource res) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/resource/reservation_form.fxml"));
            Parent root = loader.load();
            ReservationFormController controller = loader.getController();
            controller.setReservationToEdit(res);

            // Passer l'utilisateur connecté au formulaire
            controller.setCurrentUserId(currentUserId);

            MainController.getInstance().setContent(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void handleDelete(ReservationResource res) {
        // Confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la réservation");
        confirm.setContentText("Voulez-vous vraiment supprimer cette réservation ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    resService.supprimer(res.getId());
                    loadTable();
                    showAlert("Succès", "Réservation supprimée avec succès");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer: " + ex.getMessage());
                }
            }
        });
    }

    private void setupSearchAndSort() {
        FilteredList<ReservationResource> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(res -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();

                // Recherche sur le nom de la ressource et le type
                boolean matchesName = res.getResourceName() != null &&
                        res.getResourceName().toLowerCase().contains(filter);
                boolean matchesType = res.getResourceType() != null &&
                        res.getResourceType().toLowerCase().contains(filter);

                return matchesName || matchesType;
            });
        });

        SortedList<ReservationResource> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(reservationTable.comparatorProperty());
        reservationTable.setItems(sortedData);

        if (sortCombo != null) {
            sortCombo.setOnAction(e -> {
                String sort = sortCombo.getValue();
                if ("Plus récent".equals(sort)) {
                    masterData.sort((a, b) -> b.getStartTimedate().compareTo(a.getStartTimedate()));
                } else if ("Plus ancien".equals(sort)) {
                    masterData.sort((a, b) -> a.getStartTimedate().compareTo(b.getStartTimedate()));
                }
            });
        }
    }

    @FXML
    public void exportToPDF() {
        String dest = "Rapport_Reservations.pdf";
        try {
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // En-tête du rapport
            Paragraph header = new Paragraph("RAPPORT DE RÉSERVATIONS")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18)
                    .setBold();
            document.add(header);

            // Ajouter les informations de l'utilisateur
            if (UserSession.getInstance().isLoggedIn()) {
                document.add(new Paragraph("Généré par: " + UserSession.getInstance().getFullName())
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10));
                document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10));
            }

            document.add(new Paragraph("\n"));

            Table table = new Table(new float[]{3, 2, 2, 1, 1}).useAllAvailableWidth();
            String[] headers = {"Nom Ressource", "Type", "Date Début", "Date Fin", "Qté"};

            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(ColorConstants.BLUE));
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (ReservationResource r : masterData) {
                table.addCell(new Cell().add(new Paragraph(r.getResourceName() != null ? r.getResourceName() : "")));
                table.addCell(new Cell().add(new Paragraph(r.getResourceType() != null ? r.getResourceType() : "")));
                table.addCell(new Cell().add(new Paragraph(r.getStartTimedate() != null ? r.getStartTimedate().format(formatter) : "")));
                table.addCell(new Cell().add(new Paragraph(r.getEndTime() != null ? r.getEndTime().format(formatter) : "")));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(r.getQuantity()))));
            }

            document.add(table);

            // Pied de page
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Total des réservations: " + masterData.size())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold());

            document.close();

            // Ouvrir le fichier
            new ProcessBuilder("cmd", "/c", "start", dest).start();

            showAlert("Succès", "Rapport PDF généré avec succès!");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de générer le PDF: " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        loadTable();
        showAlert("Info", "Tableau rafraîchi");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    private void goToStats() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/resource/reservation_stats.fxml"));
            MainController.getInstance().setContent(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}