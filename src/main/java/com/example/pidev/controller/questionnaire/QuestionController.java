package com.example.pidev.controller.questionnaire;
import com.example.pidev.service.questionnaire.AIService;
import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.questionnaire.Question;
import com.example.pidev.service.questionnaire.QuestionService;
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

            if (ev == null || txtTexte.getText().trim().isEmpty() || txtReponse.getText().trim().isEmpty()) {
                afficherAlerte("Champs manquants", "Veuillez sélectionner un événement et remplir l'énoncé et la réponse.");
                return;
            }

            int points;
            try {
                points = Integer.parseInt(txtPoints.getText().trim());
            } catch (NumberFormatException e) {
                afficherAlerte("Format invalide", "Le champ 'Points' doit être un nombre entier.");
                return;
            }

            // FIX : ID de l'utilisateur Sellami Arij (ID 1 selon votre base de données)
            int idUtilisateurConnecte = 1;

            if (questionEnCours == null) {
                Question nouvelleQ = new Question();
                nouvelleQ.setIdEvent(ev.getId());
                nouvelleQ.setTexte(txtTexte.getText());
                nouvelleQ.setReponse(txtReponse.getText());
                nouvelleQ.setPoints(points);
                nouvelleQ.setIdUser(idUtilisateurConnecte);

                qs.ajouter(nouvelleQ);
            } else {
                questionEnCours.setIdEvent(ev.getId());
                questionEnCours.setTexte(txtTexte.getText());
                questionEnCours.setReponse(txtReponse.getText());
                questionEnCours.setPoints(points);
                questionEnCours.setIdUser(idUtilisateurConnecte);

                qs.modifier(questionEnCours);
                questionEnCours = null; // Reset après modification
            }

            switchToList();

        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte("Erreur SQL", "Impossible d'enregistrer : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Une erreur inattendue est survenue.");
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
    private void viderChampsSaufCombo() {
        if(txtTexte != null) txtTexte.clear();
        if(txtReponse != null) txtReponse.clear();
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
        if (comboEvent != null) {
            comboEvent.getItems().stream()
                    .filter(e -> e.getId() == q.getIdEvent())
                    .findFirst()
                    .ifPresent(e -> comboEvent.setValue(e));
        }
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
        // 1. Vérification si on est sur le formulaire
        if (txtTexte == null || comboEvent == null) {
            afficherAlerte("Action requise", "Veuillez d'abord ouvrir l'éditeur pour générer une question.");
            return;
        }

        // 2. Récupération de l'événement sélectionné
        Event selectedEvent = comboEvent.getValue();

        if (selectedEvent == null) {
            afficherAlerte("Sélection requise", "Veuillez choisir un événement dans la liste pour définir le thème (ex: Java, Sport...).");
            return;
        }

        // On utilise le titre de l'événement comme thème
        String theme = selectedEvent.getTitle();

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    lblSuggestionIA.setText("⌛ L'IA génère une question sur l'événement : " + theme + "...");
                    lblSuggestionIA.setStyle("-fx-text-fill: #4f46e5;");
                });

                AIService ai = new AIService();
                String jsonBrut = ai.appelerIA(theme);

                // Extraction du JSON retourné par l'IA
                org.json.JSONObject json = new org.json.JSONObject(jsonBrut);
                String question = json.getString("question");
                String reponse = json.getString("reponse");
                int points = json.optInt("points", 10);

                Platform.runLater(() -> {
                    txtTexte.setText(question);
                    txtReponse.setText(reponse);
                    txtPoints.setText(String.valueOf(points));
                    lblSuggestionIA.setText("✨ Question générée pour l'événement '" + theme + "' !");
                    lblSuggestionIA.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblSuggestionIA.setText("❌ Erreur de génération.");
                    afficherAlerte("Erreur IA", "Détail : " + e.getMessage());
                });
            }
        }).start();
    }
}
