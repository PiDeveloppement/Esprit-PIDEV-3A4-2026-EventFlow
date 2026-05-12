package com.example.pidev.model.questionnaire;

public class Feedback {
    private int idFeedback;
    private int idUser;
    private int idQuestion;
    private String reponseDonnee;
    private String comments;
    private int etoiles;

    public Feedback(int idFeedback, int idUser, int idQuestion, String reponseDonnee, String comments, int etoiles) {
        this.idFeedback = idFeedback;
        this.idUser = idUser;
        this.idQuestion = idQuestion;
        this.reponseDonnee = reponseDonnee;
        this.comments = comments;
        this.etoiles = etoiles;
    }

    // Getters et Setters
    public int getIdFeedback() { return idFeedback; }
    public void setIdFeedback(int idFeedback) { this.idFeedback = idFeedback; }
    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }
    public int getIdQuestion() { return idQuestion; }
    public void setIdQuestion(int idQuestion) { this.idQuestion = idQuestion; }
    public String getReponseDonnee() { return reponseDonnee; }
    public void setReponseDonnee(String reponseDonnee) { this.reponseDonnee = reponseDonnee; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public int getEtoiles() { return etoiles; }
    public void setEtoiles(int etoiles) { this.etoiles = etoiles; }
}