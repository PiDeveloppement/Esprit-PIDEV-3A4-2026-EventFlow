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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.awt.SystemColor.text;


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

    @FXML private Label lblSentimentPos, lblSentimentNeu, lblSentimentNeg;
    @FXML private ProgressBar sentimentBar;

    private final FeedbackService feedbackService = new FeedbackService();
    private List<Map<String, Object>> allFeedbacks = new ArrayList<>();

    public void initialize() {
        try {
            // 1. Charger les feedbacks depuis la BDD une seule fois
            allFeedbacks = feedbackService.getFeedbacksAvecDetails();

            // 2. Lancer l'analyse IA (cela remplit les clés "ai_sentiment" dans la liste)
            computeAIAnalysis(allFeedbacks);

            // 3. Récupérer les stats globales pour les KPIs
            Map<String, Object> stats = feedbackService.getStatistiquesDetaillees();
            double moyenne = (double) stats.get("moyenne");
            int total = (int) stats.get("total");
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> rep = (Map<Integer, Integer>) stats.get("repartition");

            // 4. Mettre à jour les labels de l'interface
            lblMoyenne.setText(String.format(Locale.US, "%.1f", moyenne));
            buildStarRow(starsKpi1, moyenne, 16);
            lblBasedOn.setText("Based on " + total + " reviews");
            lblTotalReviews.setText(String.valueOf(total));
            lblTrend.setText("📈 +17% from last month");

            int fiveStar = rep.getOrDefault(5, 0);
            int pct = total > 0 ? (int)((double) fiveStar / total * 100) : 0;
            lblFiveStarPct.setText(pct + "%");
            lblQuality.setText(pct >= 70 ? "👍 Excellent ratings" : "👍 Good ratings");

            lblThisMonth.setText(String.format(Locale.US, "%.1f", moyenne));
            lblMonthTrend.setText("📈 +0.1 from last month");

            buildRatingsBreakdown(rep, total);
            buildCategoryRatings(moyenne);

            // 5. ENFIN, on affiche les cartes (maintenant que l'IA a mis POSITIVE/NEGATIVE)
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
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);");

        // --- 1. INFOS UTILISATEUR & NOM ---
        String firstName = (String) fb.getOrDefault("firstName", "");
        String lastName = (String) fb.getOrDefault("lastName", "");
        String nomAffiche = (firstName + " " + lastName).trim();
        if (nomAffiche.isEmpty()) nomAffiche = "Utilisateur";

        // --- 2. BADGE IA (CORRECTION COULEUR ET TEXTE) ---
        // On récupère le sentiment calculé (POSITIVE, NEGATIVE, NEUTRAL)
        String sent = (String) fb.getOrDefault("ai_sentiment", "NEUTRAL");
        // On récupère la couleur correspondante via ta méthode getSentimentColor
        String color = getSentimentColor(sent);

        Label aiBadge = new Label(sent);
        // Le code hexadécimal de la couleur est suivi de '22' pour l'opacité du fond (effet badge)
        aiBadge.setStyle("-fx-background-color: " + color + "22; " +
                "-fx-text-fill: " + color + "; " +
                "-fx-font-size: 9px; -fx-font-weight: bold; " +
                "-fx-padding: 2 8; -fx-background-radius: 10;");

        Label lblNom = new Label(nomAffiche);
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");

        HBox nameAndBadge = new HBox(10, lblNom, aiBadge);
        nameAndBadge.setAlignment(Pos.CENTER_LEFT);

        VBox userInfos = new VBox(2, nameAndBadge);
        // Ajout de la date si elle existe dans la Map
        if (fb.get("date") != null) {
            Label lblDate = new Label("📅 " + fb.get("date").toString());
            lblDate.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");
            userInfos.getChildren().add(lblDate);
        }

        // --- 3. AVATAR CIRCULAIRE ---
        char init = nomAffiche.charAt(0);
        Label lblInit = new Label(String.valueOf(init).toUpperCase());
        lblInit.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        StackPane avatar = new StackPane(lblInit);
        avatar.setPrefSize(40, 40);
        avatar.setMinSize(40, 40);
        avatar.setStyle("-fx-background-color: " + avatarColor(init) + "; -fx-background-radius: 50;");

        // --- 4. ÉTOILES (RATING) ---
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        int etoiles = (int) fb.getOrDefault("etoiles", 0);
        HBox stars = new HBox(2);
        for(int i = 0; i < 5; i++) {
            Label s = new Label(i < etoiles ? "★" : "☆");
            s.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px;");
            stars.getChildren().add(s);
        }

        // Ligne du haut : Avatar + Nom/Badge + Spacer + Étoiles
        HBox topRow = new HBox(12, avatar, userInfos, spacer, stars);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // --- 5. COMMENTAIRE ---
        String commentaireTexte = (String) fb.getOrDefault("comments", "Aucun commentaire.");
        Label lblComment = new Label(commentaireTexte);
        lblComment.setWrapText(true);
        lblComment.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 13px; -fx-line-spacing: 4;");

        // --- 6. BOUTON SUPPRIMER ---
        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-size: 12px;");
        // On passe l'ID du feedback pour la suppression en BDD
        btnDel.setOnAction(e -> supprimerFeedback((int)fb.get("idFeedback"), card));

        // --- ASSEMBLAGE FINAL ---
        card.getChildren().addAll(topRow, lblComment, new Separator(), btnDel);
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
        String[] colors = {"#1d4ed8","#0891b2","#7c3aed","#b45309","#be185d"};
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

    private void computeAIAnalysis(List<Map<String, Object>> feedbacks) {
        int pos = 0, neu = 0, neg = 0;

        for (Map<String, Object> fb : feedbacks) {
            String comment = (String) fb.get("comments");
            if (comment == null || comment.isBlank()) {
                fb.put("ai_sentiment", "NEUTRAL");
                neu++;
                continue;
            }

            String res = feedbackService.analyzeSentiment(comment).toUpperCase();
            String finalSentiment = "NEUTRAL"; // Valeur par défaut

            // SOLUTION : On cherche quel label est écrit EN PREMIER dans la chaîne JSON
            int index1Star = res.indexOf("\"LABEL\":\"1 STAR\"");
            int index2Star = res.indexOf("\"LABEL\":\"2 STARS\"");
            int index4Star = res.indexOf("\"LABEL\":\"4 STARS\"");
            int index5Star = res.indexOf("\"LABEL\":\"5 STARS\"");

            // On identifie celui qui a l'index le plus bas (le premier dans le JSON)
            if ((index1Star != -1 && index1Star < 50) || (index2Star != -1 && index2Star < 50)) {
                finalSentiment = "NEGATIVE";
                neg++;
            }
            else if ((index4Star != -1 && index4Star < 50) || (index5Star != -1 && index5Star < 50)) {
                finalSentiment = "POSITIVE";
                pos++;
            }
            else {
                finalSentiment = "NEUTRAL";
                neu++;
            }

            fb.put("ai_sentiment", finalSentiment);
            System.out.println("Commentaire: " + comment + " => " + finalSentiment);
        }

        // --- MISE À JOUR DE L'INTERFACE ---
        int total = pos + neu + neg;
        if (total > 0) {
            // On utilise des doubles pour la précision
            double pctPos = (double) pos / total;
            double pctNeu = (double) neu / total;
            double pctNeg = (double) neg / total;

            // Mise à jour des labels (Utilisation de Math.round pour éviter les 0% bizarres)
            lblSentimentPos.setText(Math.round(pctPos * 100) + "%");
            lblSentimentNeu.setText(Math.round(pctNeu * 100) + "%");
            lblSentimentNeg.setText(Math.round(pctNeg * 100) + "%");

            // La barre de progression affiche le taux de satisfaction (Positif)
            sentimentBar.setProgress(pctPos);

            // Optionnel : Changer la couleur de la barre si c'est très négatif
            if (pctNeg > pctPos) {
                sentimentBar.setStyle("-fx-accent: #ef4444;"); // Rouge
            } else {
                sentimentBar.setStyle("-fx-accent: #22c55e;"); // Vert
            }

            System.out.println("Stats finales affichées : POS=" + pos + " | NEU=" + neu + " | NEG=" + neg);
        }
    }
    private String getSentimentColor(String sentiment) {
        if (sentiment == null) return "#94a3b8"; // Gris si nul

        // On transforme en majuscules et on enlève les espaces invisibles
        String cleanSentiment = sentiment.trim().toUpperCase();

        switch (cleanSentiment) {
            case "POSITIVE":
                return "#22c55e"; // Vert éclatant
            case "NEGATIVE":
                return "#ef4444"; // Rouge alerte
            default:
                return "#64748b"; // Gris ardoise (Neutre)
        }
    }
    private void afficherStats(Map<String, Object> stats) {
        double moyenne = (double) stats.get("moyenne");
        int total = (int) stats.get("total");

        lblMoyenne.setText(String.format(Locale.US, "%.1f", moyenne));
        lblTotalReviews.setText(String.valueOf(total));
        lblBasedOn.setText("Based on " + total + " reviews");

        buildStarRow(starsKpi1, moyenne, 16);

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> rep = (Map<Integer, Integer>) stats.get("repartition");
        buildRatingsBreakdown(rep, total);
    }

}