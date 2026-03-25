package com.example.pidev.controller.questionnaire;
import com.example.pidev.service.questionnaire.AIService;
import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.questionnaire.Question;
import com.example.pidev.service.questionnaire.QuestionService;
import com.example.pidev.service.user.UserService;
import com.example.pidev.utils.UserSession;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class QuestionController {

    @FXML private ComboBox<Event> comboEvent, comboEventList;
    @FXML private ComboBox<String> comboSort;
    @FXML private TextField txtReponse, txtPoints, txtSearch;
    @FXML private TextField txtOption1, txtOption2, txtOption3; // NOUVEAU
    @FXML private TextArea txtTexte;
    @FXML private FlowPane cardsContainer;
    @FXML private Label lblPagination, lblSuggestionIA;
    @FXML private PieChart pieChartQuestions;

    private final QuestionService qs = new QuestionService();
    private static Question questionEnCours = null;
    private List<Question> toutesLesQuestions = new ArrayList<>();
    private List<Question> questionsFiltrees = new ArrayList<>();
    private int pageActuelle = 0;
    private final int ITEMS_PER_PAGE = 6;

    @FXML
    public void initialize() {
        try {
            // 1. Initialisation du tri
            if (comboSort != null) {
                comboSort.setItems(FXCollections.observableArrayList(
                        "Points (Croissant)", "Points (Décroissant)", "Texte (A-Z)"
                ));
            }

            // 2. Chargement des données depuis la base
            List<Event> events = qs.chargerEvenements();

            // 3. Configuration du ComboBox du Formulaire (Ajout/Modification)
            if (comboEvent != null) {
                comboEvent.setItems(FXCollections.observableArrayList(events));

                // CORRECTION : Formatage de l'affichage (CellFactory et ButtonCell)
                comboEvent.setCellFactory(lv -> new ListCell<Event>() {
                    @Override
                    protected void updateItem(Event item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((empty || item == null) ? null : item.getTitle());
                    }
                });
                comboEvent.setButtonCell(new ListCell<Event>() {
                    @Override
                    protected void updateItem(Event item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((empty || item == null) ? null : item.getTitle());
                    }
                });

                if (questionEnCours != null) {
                    remplirChamps(questionEnCours);
                }

                txtTexte.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) analyserDifficulteIA(txtTexte.getText());
                });
            }

            // 4. Configuration du ComboBox de la Liste (Filtre)
            if (comboEventList != null) {
                comboEventList.setItems(FXCollections.observableArrayList(events));

                // CORRECTION : Formatage de l'affichage
                comboEventList.setCellFactory(lv -> new ListCell<Event>() {
                    @Override
                    protected void updateItem(Event item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((empty || item == null) ? null : item.getTitle());
                    }
                });

                comboEventList.setButtonCell(new ListCell<Event>() {
                    @Override
                    protected void updateItem(Event item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((empty || item == null) ? null : item.getTitle());
                    }
                });

                toutesLesQuestions = qs.afficherTout();
                questionsFiltrees = new ArrayList<>(toutesLesQuestions);
                afficherPage();

                comboEventList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) chargerQuestionsParEvent(newV.getId());
                });
            }

            // 5. Statistiques (PieChart)
            if (pieChartQuestions != null) {
                afficherStatistiques();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void analyserDifficulteIA(String texte) {
        if (texte == null || texte.trim().length() < 5 || lblSuggestionIA == null) return;

        String t = texte.toLowerCase();
        int points = 5;
        String niveau = "Facile";
        String color = "#22c55e";

        if (t.contains("pourquoi") || t.contains("comment") || t.length() > 50) {
            points = 15; niveau = "Moyenne"; color = "#f59e0b";
        }
        if (t.contains("expliquer") || t.contains("comparer") || t.contains("décrire") || t.length() > 100) {
            points = 25; niveau = "Difficile"; color = "#ef4444";
        }

        final int ptsFinal = points;
        final String nivFinal = niveau;
        final String colFinal = color;

        Platform.runLater(() -> {
            lblSuggestionIA.setText("✨ Suggestion IA : " + nivFinal + " (" + ptsFinal + " pts)");
            lblSuggestionIA.setStyle("-fx-text-fill: " + colFinal + "; -fx-font-weight: bold;");
            if (txtPoints != null) txtPoints.setText(String.valueOf(ptsFinal));
        });
    }

    @FXML
    private void handleSave() {
        try {
            Event ev = comboEvent.getValue();

            // Validation : On vérifie que les options ne sont pas vides
            if (ev == null || txtTexte.getText().trim().isEmpty() ||
                    txtReponse.getText().trim().isEmpty() ||
                    txtOption1.getText().trim().isEmpty() ||
                    txtOption2.getText().trim().isEmpty() ||
                    txtOption3.getText().trim().isEmpty()) {

                afficherAlerte("Champs manquants", "Veuillez remplir l'énoncé, la bonne réponse ET les 3 options fausses.");
                return;
            }

            int points = Integer.parseInt(txtPoints.getText().trim());
            int idUtilisateurConnecte = resolveQuestionUserId();
            if (idUtilisateurConnecte <= 0) {
                afficherAlerte("Utilisateur manquant", "Impossible de determiner un utilisateur valide pour enregistrer la question.");
                return;
            }

            if (questionEnCours == null) {
                // AJOUT : On passe les options au constructeur ou via setters
                Question nouvelleQ = new Question(
                        0, ev.getId(), txtTexte.getText(), txtReponse.getText(), points,
                        txtOption1.getText(), txtOption2.getText(), txtOption3.getText()
                );
                nouvelleQ.setIdUser(idUtilisateurConnecte);
                qs.ajouter(nouvelleQ);
            } else {
                // MODIFICATION
                questionEnCours.setIdEvent(ev.getId());
                questionEnCours.setTexte(txtTexte.getText());
                questionEnCours.setReponse(txtReponse.getText());
                questionEnCours.setPoints(points);
                questionEnCours.setOption1(txtOption1.getText());
                questionEnCours.setOption2(txtOption2.getText());
                questionEnCours.setOption3(txtOption3.getText());
                questionEnCours.setIdUser(idUtilisateurConnecte);

                qs.modifier(questionEnCours);
                questionEnCours = null;
            }

            switchToList();
        } catch (Exception e) {
            afficherAlerte("Erreur", "Vérifiez vos champs : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        if (txtSearch == null) return;
        String query = txtSearch.getText().toLowerCase().trim();
        questionsFiltrees = toutesLesQuestions.stream()
                .filter(q -> q.getTexte().toLowerCase().contains(query) || q.getReponse().toLowerCase().contains(query))
                .collect(Collectors.toList());
        pageActuelle = 0;
        afficherPage();
    }

    @FXML
    private void handleSort() {
        String criteria = comboSort.getValue();
        if (criteria == null || questionsFiltrees.isEmpty()) return;
        switch (criteria) {
            case "Points (Croissant)": questionsFiltrees.sort(Comparator.comparingInt(Question::getPoints)); break;
            case "Points (Décroissant)": questionsFiltrees.sort((q1, q2) -> Integer.compare(q2.getPoints(), q1.getPoints())); break;
            case "Texte (A-Z)": questionsFiltrees.sort(Comparator.comparing(q -> q.getTexte().toLowerCase())); break;
        }
        pageActuelle = 0;
        afficherPage();
    }

    private void chargerQuestionsParEvent(int idEvent) {
        try {
            toutesLesQuestions = qs.afficherParEvenement(idEvent);
            questionsFiltrees = new ArrayList<>(toutesLesQuestions);
            pageActuelle = 0;
            afficherPage();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void afficherPage() {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();
        int total = questionsFiltrees.size();
        int start = pageActuelle * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, total);

        for (int i = start; i < end; i++) {
            cardsContainer.getChildren().add(createPinterestCard(questionsFiltrees.get(i)));
        }

        if (lblPagination != null) {
            int totalPages = (int) Math.ceil((double) total / ITEMS_PER_PAGE);
            lblPagination.setText("Page " + (totalPages == 0 ? 0 : pageActuelle + 1) + " / " + totalPages);
        }
    }

    private VBox createPinterestCard(Question q) {
        VBox card = new VBox(12);
        card.setPrefSize(300, 180);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 12, 0, 0, 5);");

        Label lblTxt = new Label(q.getTexte());
        lblTxt.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        lblTxt.setWrapText(true);
        lblTxt.setMinHeight(50);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label pts = new Label(q.getPoints() + " pts");
        pts.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-weight: bold;");

        Button btnTranslate = new Button("🌐");
        btnTranslate.setOnAction(e -> traduireTexte(lblTxt));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnEdit = new Button("✏️");
        btnEdit.setOnAction(e -> {
            questionEnCours = q;
            switchToForm();
        });

        Button btnDel = new Button("🗑️");
        btnDel.setStyle("-fx-background-color: #fee2e2;");
        btnDel.setOnAction(e -> {
            try {
                qs.supprimer(q.getIdQuestion());
                refreshData();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        footer.getChildren().addAll(pts, btnTranslate, spacer, btnEdit, btnDel);
        card.getChildren().addAll(lblTxt, new Label("💡 " + q.getReponse()), footer);
        return card;
    }

    private void refreshData() throws SQLException {
        if(comboEventList != null && comboEventList.getValue() != null)
            chargerQuestionsParEvent(comboEventList.getValue().getId());
        else {
            toutesLesQuestions = qs.afficherTout();
            questionsFiltrees = new ArrayList<>(toutesLesQuestions);
            afficherPage();
        }
    }

    @FXML
    public void afficherStatistiques() {
        try {
            Map<String, Integer> statsMap = qs.obtenirStats();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            statsMap.forEach((nom, count) -> pieData.add(new PieChart.Data(nom + " (" + count + ")", count)));
            pieChartQuestions.setData(pieData);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void ouvrirPopupStats() { switchToStats(); }


    private void traduireTexte(Label labelAtraduire) {
        String texte = labelAtraduire.getText();
        labelAtraduire.setText("⌛...");
        new Thread(() -> {
            try {
                String encodedText = URLEncoder.encode(texte, StandardCharsets.UTF_8.toString());
                String urlStr = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=fr|en";
                URL url = new URL(urlStr);
                Scanner s = new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                String translatedText = response.split("\"translatedText\":\"")[1].split("\"")[0];
                Platform.runLater(() -> {
                    labelAtraduire.setText(translatedText);
                    labelAtraduire.setStyle("-fx-text-fill: #4f46e5; -fx-font-weight: bold;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> labelAtraduire.setText(texte));
            }
        }).start();
    }

    @FXML
    private void handleExportPDF() {
        if (questionsFiltrees.isEmpty()) {
            afficherAlerte("Export impossible", "La liste est vide.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("Questions_Export.pdf");
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                document.add(new Paragraph("LISTE DES QUESTIONS\n\n"));
                PdfPTable table = new PdfPTable(3);
                table.addCell("Question"); table.addCell("Réponse"); table.addCell("Points");
                for (Question q : questionsFiltrees) {
                    table.addCell(q.getTexte());
                    table.addCell(q.getReponse());
                    table.addCell(String.valueOf(q.getPoints()));
                }
                document.add(table);
                document.close();
                afficherAlerte("Succès", "PDF généré !");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void switchToList() {
        changeScene("/com/example/pidev/fxml/questionnaire/list_question.fxml", "📋 LISTE DES QUESTIONS");
    }

    @FXML
    private void switchToForm() {
        changeScene("/com/example/pidev/fxml/questionnaire/form_question.fxml", "⚙️ ÉDITEUR");
    }

    @FXML
    private void switchToStats() {
        changeScene("/com/example/pidev/fxml/questionnaire/stats_question.fxml", "📊 STATISTIQUES");
    }

    private void changeScene(String fxmlPath, String title) {
        Platform.runLater(() -> {
            try {
                String resourcePath = fxmlPath.startsWith("/") ? fxmlPath : "/" + fxmlPath;
                URL resource = getClass().getResource(resourcePath);

                if (resource == null) {
                    afficherAlerte("Erreur", "Fichier introuvable: " + resourcePath);
                    return;
                }

                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();

                if (MainController.getInstance() != null) {
                    MainController.getInstance().setContent(root, title);
                }

            } catch (IOException e) {
                e.printStackTrace();
                afficherAlerte("Erreur", "Impossible de charger: " + e.getMessage());
            }
        });
    }

    @FXML
    private void viderChampsSaufCombo() { // Le nom doit être identique au FXML
        if(txtTexte != null) txtTexte.clear();
        if(txtReponse != null) txtReponse.clear();
        if(txtOption1 != null) txtOption1.clear();
        if(txtOption2 != null) txtOption2.clear();
        if(txtOption3 != null) txtOption3.clear();
        if(txtPoints != null) txtPoints.clear();
        if(lblSuggestionIA != null) lblSuggestionIA.setText("");
        questionEnCours = null;
    }

    @FXML private void pageSuivante() {
        if ((pageActuelle + 1) * ITEMS_PER_PAGE < questionsFiltrees.size()) {
            pageActuelle++; afficherPage();
        }
    }

    @FXML private void pagePrecedente() {
        if (pageActuelle > 0) {
            pageActuelle--; afficherPage();
        }
    }

    private void remplirChamps(Question q) {
        txtTexte.setText(q.getTexte());
        txtReponse.setText(q.getReponse());
        txtPoints.setText(String.valueOf(q.getPoints()));

        // NOUVEAU : Remplir les options
        if(txtOption1 != null) txtOption1.setText(q.getOption1());
        if(txtOption2 != null) txtOption2.setText(q.getOption2());
        if(txtOption3 != null) txtOption3.setText(q.getOption3());

        if (comboEvent != null) {
            comboEvent.getItems().stream()
                    .filter(e -> e.getId() == q.getIdEvent())
                    .findFirst()
                    .ifPresent(e -> comboEvent.setValue(e));
        }
    }

    private int resolveQuestionUserId() {
        int sessionUserId = UserSession.getInstance().getUserId();
        if (sessionUserId > 0) {
            return sessionUserId;
        }

        try {
            UserService userService = new UserService();
            List<com.example.pidev.model.user.UserModel> users = userService.getAllUsers();
            if (users != null && !users.isEmpty()) {
                return users.get(0).getId_User();
            }
        } catch (Exception e) {
            System.err.println("Impossible de recuperer un utilisateur pour le QCM: " + e.getMessage());
        }

        return -1;
    }

    private void afficherAlerte(String t, String m) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(t);
        alert.setHeaderText(null);
        alert.setContentText(m);
        alert.show();
    }
    @FXML
    private void genererQuestionParIA() {
        if (txtTexte == null || comboEvent == null) {
            afficherAlerte("Action requise", "Veuillez d'abord ouvrir l'éditeur.");
            return;
        }

        Event selectedEvent = comboEvent.getValue();
        if (selectedEvent == null) {
            afficherAlerte("Sélection requise", "Veuillez choisir un événement.");
            return;
        }

        String theme = selectedEvent.getTitle();

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    lblSuggestionIA.setText("⌛ Génération complète du QCM...");
                    lblSuggestionIA.setStyle("-fx-text-fill: #4f46e5;");
                });

                AIService ai = new AIService();
                String reponseBruteIA = ai.appelerIA(theme);
                String jsonNettoye = extraireLeJsonUniquement(reponseBruteIA);

                org.json.JSONObject json = new org.json.JSONObject(jsonNettoye);

                // Extraction de TOUTES les données du JSON
                String question = json.optString("question", "");
                String bonneReponse = json.optString("reponse", "");
                String fausse1 = json.optString("option1", "");
                String fausse2 = json.optString("option2", "");
                String fausse3 = json.optString("option3", "");
                int points = json.optInt("points", 10);

                Platform.runLater(() -> {
                    // Remplissage de l'énoncé
                    txtTexte.setText(question);

                    // Remplissage de la bonne réponse
                    txtReponse.setText(bonneReponse);

                    // Remplissage des OPTIONS FAUSSES (Vérifie bien tes fx:id dans le contrôleur)
                    if (txtOption1 != null) txtOption1.setText(fausse1);
                    if (txtOption2 != null) txtOption2.setText(fausse2);
                    if (txtOption3 != null) txtOption3.setText(fausse3);
                    if (txtPoints != null) txtPoints.setText(String.valueOf(Math.max(1, points)));

                    lblSuggestionIA.setText("✨ QCM et options générés avec succès !");
                    lblSuggestionIA.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblSuggestionIA.setText("❌ Erreur de génération.");
                    afficherAlerte("Erreur IA", "L'IA n'a pas pu générer les options fausses.");
                });
            }
        }).start();
    }
    /**
     * Méthode utilitaire à ajouter dans ta classe pour isoler le JSON
     */
    private String extraireLeJsonUniquement(String texte) {
        try {
            int debut = texte.indexOf("{");
            int fin = texte.lastIndexOf("}");
            if (debut != -1 && fin != -1) {
                return texte.substring(debut, fin + 1);
            }
        } catch (Exception e) {
            System.err.println("Impossible d'extraire le JSON");
        }
        return texte;
    }
}
