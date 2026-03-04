package com.example.pidev.model.questionnaire;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private int idQuestion;
    private int idEvent;
    private String texte;
    private String reponse;
    private int points;
    private int idUser;
    private String nomEvent; // Pour l'affichage dans la liste

    // Options pour le QCM
    private String option1;
    private String option2;
    private String option3;

    public Question() {}

    // Constructeur complet (Utilisé pour le mapping DB)
    public Question(int idQuestion, int idEvent, String texte, String reponse, int points, String o1, String o2, String o3) {
        this.idQuestion = idQuestion;
        this.idEvent = idEvent;
        this.texte = texte;
        this.reponse = reponse;
        this.points = points;
        this.option1 = o1;
        this.option2 = o2;
        this.option3 = o3;
    }

    // --- Getters et Setters ---
    public int getIdQuestion() { return idQuestion; }
    public void setIdQuestion(int idQuestion) { this.idQuestion = idQuestion; }

    public int getIdEvent() { return idEvent; }
    public void setIdEvent(int idEvent) { this.idEvent = idEvent; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getNomEvent() { return nomEvent; }
    public void setNomEvent(String nomEvent) { this.nomEvent = nomEvent; }

    public String getOption1() { return option1; }
    public void setOption1(String option1) { this.option1 = option1; }

    public String getOption2() { return option2; }
    public void setOption2(String option2) { this.option2 = option2; }

    public String getOption3() { return option3; }
    public void setOption3(String option3) { this.option3 = option3; }

    // Méthode pour récupérer toutes les options (utile pour le ParticipantController)
    public List<String> getOptions() {
        List<String> opts = new ArrayList<>();
        opts.add(reponse);
        opts.add(option1);
        opts.add(option2);
        opts.add(option3);
        return opts;
    }
}