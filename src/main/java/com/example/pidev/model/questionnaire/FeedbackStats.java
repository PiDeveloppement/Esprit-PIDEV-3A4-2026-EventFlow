package com.example.pidev.model.questionnaire;

public class FeedbackStats {
    private int id;
    private String username;
    private String commentaire;
    private String score;
    private int etoiles;

    public FeedbackStats(int id, String username, String commentaire, String score, int etoiles) {
        this.id = id;
        this.username = username;
        this.commentaire = commentaire;
        this.score = score;
        this.etoiles = etoiles;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getCommentaire() { return commentaire; }
    public String getScore() { return score; }
    public int getEtoiles() { return etoiles; }
}