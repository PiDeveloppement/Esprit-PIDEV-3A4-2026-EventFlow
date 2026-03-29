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

    @FXML
    private PieChart pieChart;

    private final ReservationService reservationService = new ReservationService();

    @FXML
    public void initialize() {
        setupChart();
    }

    private void setupChart() {
        Map<String, Integer> statsByType = reservationService.getStatsByType();

        if (statsByType == null || statsByType.isEmpty()) {
            pieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Aucune reservation", 1)
            ));
            pieChart.setTitle("Aucune donnee a afficher");
            pieChart.setLegendVisible(false);
            pieChart.setLabelsVisible(true);
            pieChart.setClockwise(true);
            return;
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        statsByType.forEach((label, value) ->
                pieChartData.add(new PieChart.Data(label + " (" + value + ")", value))
        );

        pieChart.setData(pieChartData);
        pieChart.setLegendVisible(true);
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
