package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.service.resource.ReservationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import java.util.Map;

public class StatsController {

    @FXML private PieChart pieChart; // Utilisation du composant natif
    private final ReservationService resService = new ReservationService();

    @FXML
    public void initialize() {
        setupChart();
    }

    private void setupChart() {
        // 1. Récupérer les données de ton service
        Map<String, Integer> typeData = resService.getStatsByType();

        // 2. Transformer les données pour le graphique
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        typeData.forEach((label, value) -> {
            pieChartData.add(new PieChart.Data(label + " (" + value + ")", value));
        });

        // 3. Afficher les données
        pieChart.setData(pieChartData);

        // Optionnel : Ajouter des labels visibles
        pieChart.setLabelsVisible(true);
        pieChart.setClockwise(true);
    }

    @FXML
    private void goBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/resource/reservation.fxml"));
            MainController.getInstance().setContent(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}