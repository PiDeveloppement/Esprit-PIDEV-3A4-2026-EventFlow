package com.example.pidev.model.questionnaire;

public class Question {
    private int idQuestion;
    private int idEvent;
    private String texte;
    private String reponse;
    private int points;
    private int idUser;      // AJOUT : Indispensable pour la DB (clé étrangère)
    private String nomEvent; // Harmonisé avec le service

    // 1. Constructeur par défaut (Utile pour l'instanciation vide)
    public Question() {}

    // 2. Constructeur avec arguments
    public Question(int idQuestion, int idEvent, String texte, String reponse, int points) {
        this.idQuestion = idQuestion;
        this.idEvent = idEvent;
        this.texte = texte;
        this.reponse = reponse;
        this.points = points;
    }

    // --- GETTERS ---
    public int getIdQuestion() { return idQuestion; }
    public int getIdEvent() { return idEvent; }
    public String getTexte() { return texte; }
    public String getReponse() { return reponse; }
    public int getPoints() { return points; }
    public String getNomEvent() { return nomEvent; }
    public int getIdUser() { return idUser; } // AJOUT

    // --- SETTERS ---
    public void setIdQuestion(int idQuestion) { this.idQuestion = idQuestion; }
    public void setIdEvent(int idEvent) { this.idEvent = idEvent; }
    public void setTexte(String texte) { this.texte = texte; }
    public void setReponse(String reponse) { this.reponse = reponse; }
    public void setPoints(int points) { this.points = points; }
    public void setNomEvent(String nomEvent) { this.nomEvent = nomEvent; }
    public void setIdUser(int idUser) { this.idUser = idUser; } // AJOUT

    // --- COMPATIBILITÉ SERVICE ---
    public String getTexteQuestion() { return texte; }
    public String getBonneReponse() { return reponse; }
}