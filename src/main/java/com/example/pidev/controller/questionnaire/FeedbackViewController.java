package com.example.pidev.controller.questionnaire;

import com.example.pidev.service.questionnaire.FeedbackService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.stream.Collectors;
import java.sql.SQLException;
import java.util.*;
import java.util.Locale;
import javafx.stage.FileChooser;
import java.io.File;

public class FeedbackViewController {

    @FXML private HBox   starsKpi1;
    @FXML private Label  lblMoyenne;
    @FXML private Label  lblBasedOn;
    @FXML private Label  lblTotalReviews;
    @FXML private Label  lblTrend;
    @FXML private Label  lblFiveStarPct;
    @FXML private Label  lblQuality;
    @FXML private Label  lblThisMonth;
    @FXML private Label  lblMonthTrend;
    @FXML private VBox   ratingsBreakdownBox;
    @FXML private VBox   categoryRatingsBox;
    @FXML private VBox   feedbackListContainer;
    @FXML private Button btnTabRecent;
    @FXML private Button btnTabHighest;
    @FXML private Button btnTabPhotos;

    private final FeedbackService feedbackService = new FeedbackService();
    private List<Map<String, Object>> allFeedbacks = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            Map<String, Object> stats = feedbackService.getStatistiquesDetaillees();
            double moyenne = (double) stats.get("moyenne");
            int    total   = (int)    stats.get("total");
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> rep = (Map<Integer, Integer>) stats.get("repartition");

            lblMoyenne.setText(String.format(Locale.US, "%.1f", moyenne));
            buildStarRow(starsKpi1, moyenne, 16);
            lblBasedOn.setText("Based on " + total + " reviews");

            lblTotalReviews.setText(String.valueOf(total));
            lblTrend.setText("📈  +17% from last month");

            int fiveStar = rep.getOrDefault(5, 0);
            int pct = total > 0 ? (int)((double) fiveStar / total * 100) : 0;
            lblFiveStarPct.setText(pct + "%");
            lblQuality.setText(pct >= 70 ? "👍  Excellent ratings" : "👍  Good ratings");

            lblThisMonth.setText(String.format(Locale.US, "%.1f", moyenne));
            lblMonthTrend.setText("📈  +0.1 from last month");

            buildRatingsBreakdown(rep, total);
            buildCategoryRatings(moyenne);

            allFeedbacks = feedbackService.getFeedbacksAvecDetails();
            displayFeedbacks(allFeedbacks);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les feedbacks: " + e.getMessage());
        }
    }

    @FXML private void showRecentTab() {
        setActiveTab(btnTabRecent, btnTabHighest, btnTabPhotos);
        displayFeedbacks(allFeedbacks);
    }

    @FXML private void showHighestTab() {
        setActiveTab(btnTabHighest, btnTabRecent, btnTabPhotos);
        List<Map<String, Object>> sorted = new ArrayList<>(allFeedbacks);
        sorted.sort((a, b) -> Integer.compare((int) b.get("etoiles"), (int) a.get("etoiles")));
        displayFeedbacks(sorted);
    }

    @FXML private void showPhotosTab() {
        setActiveTab(btnTabPhotos, btnTabRecent, btnTabHighest);
        displayFeedbacks(allFeedbacks);
    }

    private void setActiveTab(Button active, Button... inactives) {
        active.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #1d4ed8; " +
                        "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-padding: 13 16; -fx-border-color: #1d4ed8; -fx-border-width: 0 0 2.5 0;");
        for (Button b : inactives) {
            b.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #6b7280; " +
                            "-fx-font-size: 13px; -fx-cursor: hand; " +
                            "-fx-padding: 13 16; -fx-border-color: transparent; -fx-border-width: 0 0 2.5 0;");
        }
    }

    private void buildRatingsBreakdown(Map<Integer, Integer> rep, int total) {
        ratingsBreakdownBox.getChildren().clear();
        int maxVal = total > 0 ? total : 1;

        for (int i = 5; i >= 1; i--) {
            int nb = rep.getOrDefault(i, 0);

            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);

            Label lblNum = new Label(String.valueOf(i));
            lblNum.setPrefWidth(14);
            lblNum.setMinWidth(14);
            lblNum.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");

            Label star = new Label("★");
            star.setPrefWidth(18);
            star.setMinWidth(18);
            star.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px;");

            Pane barBg = new Pane();
            barBg.setPrefWidth(170);
            barBg.setMinWidth(170);
            barBg.setMaxWidth(170);
            barBg.setPrefHeight(8);
            barBg.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 4;");

            double ratio = (double) nb / maxVal;
            if (ratio > 0) {
                Pane fill = new Pane();
                fill.setPrefHeight(8);
                fill.setPrefWidth(170 * ratio);
                fill.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 4;");
                barBg.getChildren().add(fill);
            }

            Label count = new Label(String.valueOf(nb));
            count.setPrefWidth(32);
            count.setMinWidth(32);
            count.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

            row.getChildren().addAll(lblNum, star, barBg, count);
            ratingsBreakdownBox.getChildren().add(row);
        }
    }

    private void buildCategoryRatings(double base) {
        categoryRatingsBox.getChildren().clear();

        String[] names = {"Professionalism", "Quality of work", "Timeliness", "Communication"};
        double[] vals  = {
                Math.min(5.0, base + 0.1),
                Math.min(5.0, base),
                Math.max(1.0, base - 0.1),
                Math.min(5.0, base + 0.05)
        };

        for (int i = 0; i < names.length; i++) {
            double val = vals[i];
            int fullStars = (int) Math.round(val);

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(names[i]);
            name.setPrefWidth(120);
            name.setMinWidth(120);
            name.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");

            Label score = new Label(String.format(Locale.US, "%.1f", val));
            score.setPrefWidth(30);
            score.setMinWidth(30);
            score.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827; -fx-font-size: 12px;");

            HBox starsBox = new HBox(2);
            starsBox.setAlignment(Pos.CENTER_LEFT);
            starsBox.setMinWidth(90);
            starsBox.setPrefWidth(90);
            for (int s = 0; s < 5; s++) {
                Label sl = new Label(s < fullStars ? "★" : "☆");
                sl.setPrefWidth(16);
                sl.setMinWidth(16);
                sl.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 15px;");
                starsBox.getChildren().add(sl);
            }

            row.getChildren().addAll(name, score, starsBox);
            categoryRatingsBox.getChildren().add(row);
        }
    }

    private void displayFeedbacks(List<Map<String, Object>> feedbacks) {
        feedbackListContainer.getChildren().clear();
        String lastEvent = "";
        for (Map<String, Object> fb : feedbacks) {
            String nomEvent = fb.get("nomEvent") != null ?
                    (String) fb.get("nomEvent") : "Événement inconnu";
            if (!nomEvent.equals(lastEvent)) {
                lastEvent = nomEvent;
                feedbackListContainer.getChildren().add(buildEventHeader(nomEvent));
            }
            feedbackListContainer.getChildren().add(buildFeedbackCard(fb));
        }
    }

    private HBox buildEventHeader(String nom) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 4 0 2 0;");

        Label icon = new Label("📅");
        icon.setStyle("-fx-font-size: 13px;");
        Label title = new Label(nom);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);

        box.getChildren().addAll(icon, title, sep);
        return box;
    }

    private VBox buildFeedbackCard(Map<String, Object> fb) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-padding: 16 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.1), 12, 0, 0, 4); " +
                        "-fx-border-color: #dbeafe; -fx-border-width: 1; -fx-border-radius: 12;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);"));

        String firstName = fb.get("firstName") != null ? (String) fb.get("firstName") : "";
        String lastName  = fb.get("lastName")  != null ? (String) fb.get("lastName")  : "";
        String initiale  = !firstName.isEmpty() ?
                String.valueOf(firstName.charAt(0)).toUpperCase() : "U";
        int etoiles = fb.get("etoiles") != null ? (int) fb.get("etoiles") : 0;

        StackPane avatar = new StackPane();
        avatar.setPrefSize(44, 44);
        avatar.setMinSize(44, 44);
        avatar.setStyle("-fx-background-color: " + avatarColor(initiale.charAt(0)) +
                "; -fx-background-radius: 50;");
        Label lblInit = new Label(initiale);
        lblInit.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
        avatar.getChildren().add(lblInit);

        String nomAffiche = (firstName + " " + lastName).trim();
        if (nomAffiche.isEmpty()) nomAffiche = "Utilisateur";

        Label lblNom = new Label(nomAffiche);
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");

        VBox nameBox = new VBox(2);
        nameBox.getChildren().add(lblNom);
        if (fb.get("date") != null) {
            Label dateL = new Label("📅 " + fb.get("date").toString());
            dateL.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
            nameBox.getChildren().add(dateL);
        }

        HBox starsRight = new HBox(2);
        starsRight.setAlignment(Pos.CENTER_RIGHT);
        for (int s = 0; s < 5; s++) {
            Label sl = new Label(s < etoiles ? "★" : "☆");
            sl.setPrefWidth(16);
            sl.setMinWidth(16);
            sl.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 15px;");
            starsRight.getChildren().add(sl);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(avatar, nameBox, spacer, starsRight);

        String comments = (String) fb.get("comments");
        Label lblComment = new Label(
                comments != null && !comments.isBlank() ? comments : "Aucun commentaire.");
        lblComment.setWrapText(true);
        lblComment.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 13px;");

        GridPane catGrid = new GridPane();
        catGrid.setHgap(30);
        catGrid.setVgap(8);

        String[] catNames = {"Professional", "Timeliness", "Quality of work", "Communication"};
        int[] catVals = {
                Math.min(5, etoiles),
                Math.max(1, etoiles - 1),
                Math.min(5, etoiles),
                Math.min(5, etoiles)
        };

        int col = 0, rowIdx = 0;
        for (int i = 0; i < catNames.length; i++) {
            HBox catItem = new HBox(6);
            catItem.setAlignment(Pos.CENTER_LEFT);

            Label catName = new Label(catNames[i]);
            catName.setPrefWidth(105);
            catName.setMinWidth(105);
            catName.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

            HBox catStars = new HBox(2);
            for (int s = 0; s < 5; s++) {
                Label sl = new Label(s < catVals[i] ? "★" : "☆");
                sl.setPrefWidth(15);
                sl.setMinWidth(15);
                sl.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 13px;");
                catStars.getChildren().add(sl);
            }

            catItem.getChildren().addAll(catName, catStars);
            catGrid.add(catItem, col, rowIdx);
            col++;
            if (col > 1) { col = 0; rowIdx++; }
        }

        Label helpful = new Label("👍  8 people found this helpful");
        helpful.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        Button btnDelete = new Button("🗑  Supprimer");
        btnDelete.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                        "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");

        btnDelete.setOnAction(e -> supprimerFeedback((int)fb.get("idFeedback"), card));

        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.getChildren().addAll(helpful, sp2, btnDelete);

        card.getChildren().addAll(topRow, lblComment, catGrid, new Separator(), bottomRow);
        return card;
    }

    private void buildStarRow(HBox container, double rating, int size) {
        container.getChildren().clear();
        int full = (int) Math.round(rating);
        for (int i = 0; i < 5; i++) {
            Label s = new Label(i < full ? "★" : "☆");
            s.setPrefWidth(size);
            s.setMinWidth(size);
            s.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: " + size + "px;");
            container.getChildren().add(s);
        }
    }

    private String avatarColor(char c) {
        String[] colors = {
                "#1d4ed8","#0891b2","#0369a1","#1e40af",
                "#7c3aed","#6d28d9","#059669","#047857",
                "#b45309","#c2410c","#be185d","#0f766e"
        };
        return colors[Math.abs(c % colors.length)];
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
        java.io.File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                writer.println("Nom,Etoiles,Commentaire");
                for (Map<String, Object> fb : allFeedbacks) {
                    writer.println(fb.get("firstName") + "," + fb.get("etoiles") + ",\"" + fb.get("comments") + "\"");
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exporté avec succès: " + file.getAbsolutePath());
                alert.show();
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Erreur", "Erreur lors de l'export: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleFilter() {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(3, Arrays.asList(1, 2, 3, 4, 5));
        dialog.setTitle("Filtre des avis");
        dialog.setHeaderText("Afficher les avis avec au moins :");

        Optional<Integer> result = dialog.showAndWait();
        if (result.isPresent()) {
            int noteMin = result.get();
            List<Map<String, Object>> filtered = allFeedbacks.stream()
                    .filter(f -> (int) f.get("etoiles") >= noteMin)
                    .collect(Collectors.toList());
            displayFeedbacks(filtered);
        }
    }

    private void supprimerFeedback(int idFeedback, VBox card) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'avis");
        alert.setHeaderText("Voulez-vous vraiment supprimer cet avis ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    feedbackService.supprimerFeedback(idFeedback);
                    feedbackListContainer.getChildren().remove(card);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage());
                }
            }
        });
    }

    // Méthode utilitaire pour les alertes
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}