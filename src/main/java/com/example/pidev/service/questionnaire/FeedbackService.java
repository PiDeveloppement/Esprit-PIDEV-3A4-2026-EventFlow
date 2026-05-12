package com.example.pidev.service.questionnaire;

import com.example.pidev.model.questionnaire.Feedback;
import com.example.pidev.model.questionnaire.FeedbackStats;
import com.example.pidev.model.questionnaire.Question;
import com.example.pidev.utils.DBConnection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class FeedbackService {
    private final Connection conn = DBConnection.getInstance().getCnx();

    public List<Question> chargerQuestionsAleatoires(int idEvent) throws SQLException {
        List<Question> questions = new ArrayList<>();
        // On ajoute option1, option2, option3 à la requête
        String req = "SELECT id_question, id_event, texte_question, bonne_reponse, points, option1, option2, option3 FROM questions WHERE id_event = ? ORDER BY RAND() LIMIT 10";

        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, idEvent);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(
                            rs.getInt("id_question"),
                            rs.getInt("id_event"),
                            rs.getString("texte_question"),
                            rs.getString("bonne_reponse"),
                            rs.getInt("points"),
                            rs.getString("option1"),
                            rs.getString("option2"),
                            rs.getString("option3")
                    ));
                }
            }
        }
        return questions;
    }

    public int enregistrerFeedbackComplet(int idUser, int idEvent, int idQuest, String rep, String comm, int stars) throws SQLException {
        String req = "INSERT INTO feedbacks (id_user, id_event, id_question, reponse_donnee, comments, etoiles) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, idUser);
            pst.setInt(2, idEvent);
            pst.setInt(3, idQuest);
            pst.setString(4, rep);
            pst.setString(5, comm);
            pst.setInt(6, stars);
            pst.executeUpdate();
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<FeedbackStats> recupererHistorique() throws SQLException {
        List<FeedbackStats> historique = new ArrayList<>();
        String req = "SELECT f.id_feedback, f.comments, f.etoiles, u.username " +
                "FROM feedbacks f JOIN user_model u ON f.id_user = u.id_user";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                historique.add(new FeedbackStats(
                        rs.getInt("id_feedback"), rs.getString("username"),
                        rs.getString("comments") != null ? rs.getString("comments") : "Pas de commentaire",
                        "N/A", rs.getInt("etoiles")
                ));
            }
        }
        return historique;
    }

    public void modifierFeedback(int idFeedback, String comment, int stars) throws SQLException {
        String query = "UPDATE feedbacks SET comments = ?, etoiles = ? WHERE id_feedback = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, comment);
            pst.setInt(2, stars);
            pst.setInt(3, idFeedback);
            pst.executeUpdate();
        }
    }

    public void supprimerFeedback(int idFeedback) throws SQLException {
        String query = "DELETE FROM feedbacks WHERE id_feedback = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setInt(1, idFeedback);
            pst.executeUpdate();
        }
    }

    public Map<String, Object> getStatistiques() throws SQLException {
        String sql = "SELECT AVG(etoiles) as moyenne, COUNT(*) as total FROM feedbacks";
        Map<String, Object> stats = new HashMap<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                stats.put("moyenne", rs.getDouble("moyenne"));
                stats.put("total", rs.getInt("total"));
            }
        }
        return stats;
    }

    public List<Feedback> getListeFeedbacks() throws SQLException {
        List<Feedback> list = new ArrayList<>();
        String sql = "SELECT * FROM feedbacks";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Feedback(
                        rs.getInt("id_feedback"), rs.getInt("id_user"), rs.getInt("id_question"),
                        rs.getString("reponse_donnee"), rs.getString("comments"), rs.getInt("etoiles")
                ));
            }
        }
        return list;
    }

    // ==================== MÉTHODES POUR LA LANDING PAGE ====================

    /**
     * Récupère les feedbacks avec prénom, nom du user et titre de l'event
     * JOIN direct feedbacks -> users (id_user) + feedbacks -> event (id_event)
     */
    public List<Map<String, Object>> getFeedbacksAvecDetails() throws SQLException {
        List<Map<String, Object>> liste = new ArrayList<>();
        // Prendre UN seul feedback par utilisateur (le dernier), groupé par id_user
        String sql =
                "SELECT f.id_feedback, f.comments, f.etoiles, f.id_user, " +
                        "u.First_Name, u.Last_Name " +
                        "FROM feedbacks f " +
                        "LEFT JOIN user_model u ON f.id_user = u.Id_User " +
                        "WHERE f.id_feedback IN (" +
                        "   SELECT MAX(id_feedback) FROM feedbacks GROUP BY id_user" +
                        ") " +
                        "ORDER BY f.etoiles DESC, f.id_feedback DESC";

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("idFeedback", rs.getInt("id_feedback"));
                String firstName = rs.getString("First_Name");
                String lastName  = rs.getString("Last_Name");
                map.put("firstName", firstName != null ? firstName : "Utilisateur");
                map.put("lastName",  lastName  != null ? lastName  : "");
                map.put("comments",  rs.getString("comments"));
                map.put("etoiles",   rs.getInt("etoiles"));
                map.put("nomEvent",  "EventFlow");
                liste.add(map);
            }
        }
        return liste;
    }

    /**
     * Statistiques globales + répartition par nombre d'étoiles (1 à 5)
     */
    public Map<String, Object> getStatistiquesDetaillees() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        String sql1 = "SELECT AVG(etoiles) as moyenne, COUNT(*) as total FROM feedbacks";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql1)) {
            if (rs.next()) {
                stats.put("moyenne", rs.getDouble("moyenne"));
                stats.put("total",   rs.getInt("total"));
            }
        }

        String sql2 = "SELECT etoiles, COUNT(*) as nb FROM feedbacks GROUP BY etoiles";
        Map<Integer, Integer> repartition = new HashMap<>();
        for (int i = 1; i <= 5; i++) repartition.put(i, 0);

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql2)) {
            while (rs.next()) {
                int etoiles = rs.getInt("etoiles");
                if (etoiles >= 1 && etoiles <= 5)
                    repartition.put(etoiles, rs.getInt("nb"));
            }
        }
        stats.put("repartition", repartition);
        return stats;
    }
    public String getNomUserComplet(int idUser) {
        String nomComplet = "";
        // Note : On utilise 'user_model' et les colonnes 'First_Name'/'Last_Name'
        // comme vu dans tes autres méthodes
        String sql = "SELECT First_Name, Last_Name FROM user_model WHERE Id_User = ?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, idUser);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String prenom = rs.getString("First_Name");
                    String nom = rs.getString("Last_Name");

                    nomComplet = (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
                    nomComplet = nomComplet.trim();
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du nom utilisateur : " + e.getMessage());
        }

        // Si on ne trouve rien en BDD, on retourne une valeur par défaut pour ne pas avoir un PDF vide
        return nomComplet.isEmpty() ? "Participant #" + idUser : nomComplet;
    }
    public String analyzeSentiment(String text) {
        if (text == null || text.isBlank()) return "NEUTRAL";

        try {
            // 1. Ton nouveau Token (Assure-toi qu'il est valide)
            String API_TOKEN = System.getenv("comment_API_KEY");



            // 2. NOUVELLE URL ROUTER (Format obligatoire maintenant)
            String API_URL = "https://router.huggingface.co/hf-inference/models/nlptown/bert-base-multilingual-uncased-sentiment";

            String cleanedText = text.replace("\"", "\\\"").replace("\n", " ");
            String jsonInput = "{\"inputs\": \"" + cleanedText + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_TOKEN)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().toUpperCase();

            System.out.println("DEBUG API STATUS " + response.statusCode() + " : " + body);

            // Si le modèle charge (Error 503), on renvoie "LOADING"
            if (body.contains("LOADING") || body.contains("ESTIMATED_TIME")) {
                return "LOADING";
            }

            if (response.statusCode() == 200) {
                return body;
            }
        } catch (Exception e) {
            System.err.println("Erreur IA: " + e.getMessage());
        }
        return "NEUTRAL";
    }
}