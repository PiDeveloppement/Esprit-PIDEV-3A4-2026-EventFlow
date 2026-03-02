package com.example.pidev.utils;

import com.example.pidev.model.user.UserModel;

public class UserSession {

    // Instance unique (pattern Singleton)
    private static UserSession instance;

    // Utilisateur connecté
    private UserModel currentUser;

    // Événement en attente de participation (avant connexion)
    private Integer pendingEventId;

    // Constructeur privé (empêche l'instanciation directe)
    private UserSession() {}

    // Point d'accès unique à l'instance
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // Stocker l'utilisateur connecté
    public void setCurrentUser(UserModel user) {
        this.currentUser = user;
        System.out.println("✅ Utilisateur connecté: " +
                (user != null ? user.getEmail() : "null"));
    }

    // Récupérer l'utilisateur connecté
    public UserModel getCurrentUser() {
        return currentUser;
    }

    // Vérifier si un utilisateur est connecté
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Déconnexion
    public void clearSession() {
        System.out.println("👋 Déconnexion de: " +
                (currentUser != null ? currentUser.getEmail() : "inconnu"));
        this.currentUser = null;
    }

    // ===== Méthodes utilitaires =====

    // Récupérer le nom complet
    public String getFullName() {
        if (currentUser != null) {
            String firstName = currentUser.getFirst_Name() != null ? currentUser.getFirst_Name() : "";
            String lastName = currentUser.getLast_Name() != null ? currentUser.getLast_Name() : "";
            return (firstName + " " + lastName).trim();
        }
        return "Invité";
    }

    // Récupérer le prénom
    public String getFirstName() {
        return currentUser != null ? currentUser.getFirst_Name() : "";
    }

    // Récupérer le nom
    public String getLastName() {
        return currentUser != null ? currentUser.getLast_Name() : "";
    }

    // Récupérer l'email
    public String getEmail() {
        return currentUser != null ? currentUser.getEmail() : "";
    }

    // Récupérer le rôle
    public String getRole() {
        if (currentUser != null && currentUser.getRole() != null) {
            return currentUser.getRole().getRoleName();
        }
        return null;
    }

    // Récupérer l'ID du rôle
    public int getRoleId() {
        return currentUser != null ? currentUser.getRole_Id() : -1;
    }

    // Récupérer l'ID utilisateur
    public int getUserId() {
        return currentUser != null ? currentUser.getId_User() : -1;
    }

    // Récupérer les initiales (pour l'avatar)
    public String getInitials() {
        if (currentUser != null) {
            String first = currentUser.getFirst_Name() != null && !currentUser.getFirst_Name().isEmpty()
                    ? currentUser.getFirst_Name().substring(0, 1) : "";
            String last = currentUser.getLast_Name() != null && !currentUser.getLast_Name().isEmpty()
                    ? currentUser.getLast_Name().substring(0, 1) : "";
            return (first + last).toUpperCase();
        }
        return "U";
    }

    // Récupérer l'URL de la photo de profil
    public String getProfilePictureUrl() {
        return currentUser != null ? currentUser.getProfilePictureUrl() : null;
    }

    // Récupérer le téléphone
    public String getPhone() {
        return currentUser != null ? currentUser.getPhone() : "";
    }

    // Mettre à jour les informations (après modification du profil)
    public void updateUserInfo(UserModel updatedUser) {
        if (updatedUser != null && currentUser != null &&
                currentUser.getId_User() == updatedUser.getId_User()) {
            this.currentUser = updatedUser;
            System.out.println("✅ Informations utilisateur mises à jour");
        }
    }

    // Affichage des informations (pour debug)
    public void printSessionInfo() {
        if (currentUser != null) {
            System.out.println("=== SESSION UTILISATEUR ===");
            System.out.println("ID: " + currentUser.getId_User());
            System.out.println("Nom: " + getFullName());
            System.out.println("Email: " + currentUser.getEmail());
            System.out.println("Rôle: " + getRole());
            System.out.println("===========================");
        } else {
            System.out.println("❌ Aucun utilisateur connecté");
        }
    }

    // ===== Gestion de l'événement en attente =====

    public void setPendingEventId(int eventId) {
        this.pendingEventId = eventId;
        System.out.println("📌 Événement en attente: " + eventId);
    }

    public Integer getPendingEventId() {
        return pendingEventId;
    }

    public boolean hasPendingEvent() {
        return pendingEventId != null;
    }

    public void clearPendingEventId() {
        System.out.println("🗑️ Nettoyage événement en attente: " + pendingEventId);
        this.pendingEventId = null;
    }
}
