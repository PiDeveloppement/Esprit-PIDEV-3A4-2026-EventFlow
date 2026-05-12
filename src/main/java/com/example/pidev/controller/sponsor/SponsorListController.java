package com.example.pidev.controller.sponsor;

import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SponsorListController implements Initializable {

    @FXML private TableView<Sponsor> table;

    @FXML private TableColumn<Sponsor, String> colId;
    @FXML private TableColumn<Sponsor, String> colEventName;
    @FXML private TableColumn<Sponsor, String> colCompany;
    @FXML private TableColumn<Sponsor, String> colEmail;
    @FXML private TableColumn<Sponsor, String> colContribution;
    @FXML private TableColumn<Sponsor, String> colContract;

    @FXML private TextField searchField;

    private final SponsorService sponsorService = new SponsorService();

    private final ObservableList<Sponsor> baseList = FXCollections.observableArrayList();
    private FilteredList<Sponsor> filtered;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // ⚠️ Si ton FXML n'a pas ces fx:id, table sera null -> NPE
        if (table == null) return;

        setupColumns();

        filtered = new FilteredList<>(baseList, s -> true);

        SortedList<Sponsor> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        }

        refresh();
    }

    private void setupColumns() {

        if (colId != null) {
            colId.setCellValueFactory(c ->
                    new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        }

        if (colCompany != null) {
            colCompany.setCellValueFactory(c ->
                    new SimpleStringProperty(nvl(c.getValue().getCompany_name())));
        }

        if (colEmail != null) {
            colEmail.setCellValueFactory(c ->
                    new SimpleStringProperty(nvl(c.getValue().getContact_email())));
        }

        if (colContribution != null) {
            colContribution.setCellValueFactory(c ->
                    new SimpleStringProperty(String.format("%,.2f", c.getValue().getContribution_name())));
        }

        if (colContract != null) {
            colContract.setCellValueFactory(c ->
                    new SimpleStringProperty(nvl(c.getValue().getContract_url())));
        }

        // Event name: si tu n’as pas join Event dans SponsorService,
        // laisse vide ou affiche event_id.
        if (colEventName != null) {
            colEventName.setCellValueFactory(c ->
                    new SimpleStringProperty(String.valueOf(c.getValue().getEvent_id())));
        }
    }

    private void refresh() {
        try {
            baseList.setAll(sponsorService.getAllSponsors());
            applyFilter();
        } catch (SQLException e) {
            showError("DB", "Erreur chargement sponsors: " + e.getMessage());
        }
    }

    private void applyFilter() {
        if (filtered == null) return;

        String q = (searchField == null || searchField.getText() == null)
                ? "" : searchField.getText().trim().toLowerCase();

        filtered.setPredicate(s -> {
            if (q.isEmpty()) return true;

            return String.valueOf(s.getId()).contains(q)
                    || nvl(s.getCompany_name()).toLowerCase().contains(q)
                    || nvl(s.getContact_email()).toLowerCase().contains(q)
                    || String.valueOf(s.getEvent_id()).contains(q);
        });
    }

    // Exemple actions (si tu as des boutons/menu)
    @FXML
    private void handleDelete() {
        Sponsor s = (table == null) ? null : table.getSelectionModel().getSelectedItem();
        if (s == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer sponsor");
        confirm.setContentText("Supprimer: " + s.getCompany_name() + " (id=" + s.getId() + ") ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    sponsorService.deleteSponsor(s.getId());
                    refresh();
                } catch (SQLException e) {
                    showError("DB", "Erreur suppression: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
