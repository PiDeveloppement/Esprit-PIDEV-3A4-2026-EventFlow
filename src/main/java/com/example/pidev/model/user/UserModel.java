package com.example.pidev.model.user;

import com.example.pidev.model.role.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;

public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id_User;

    private String first_Name;
    private String last_Name;
    private String email;
    private String faculte;
    private String password;
    private String confirmerPassword;
    private String roleName;

    // NOUVEAUX CHAMPS POUR LE PROFIL
    private String phone;                  // Téléphone
    private String profilePictureUrl;       // URL de la photo de profil
    private LocalDateTime registrationDate; // Date d'inscription
    private String bio;                     // Biographie

    @ManyToOne
    @JoinColumn(name="id_role")
    private Role role;

    private int role_Id;

    // ================= CONSTRUCTEURS =================

    public UserModel() {}

    // Constructeur complet avec tous les champs
    public UserModel(int id_User, String firstName, String last_Name, String email,
                     String faculte, String password, int role_Id,
                     String phone, String profilePictureUrl,
                     LocalDateTime registrationDate, String bio) {
        this.id_User = id_User;
        this.first_Name = firstName;
        this.last_Name = last_Name;
        this.email = email;
        this.faculte = faculte;
        this.password = password;
        this.role_Id = role_Id;
        this.phone = phone;
        this.profilePictureUrl = profilePictureUrl;
        this.registrationDate = registrationDate;
        this.bio = bio;
    }

    // Constructeur sans ID (pour création)
    public UserModel(String firstName, String last_Name, String email,
                     String faculte, String password, int role_Id,
                     String phone, String profilePictureUrl, String bio) {
        this.first_Name = firstName;
        this.last_Name = last_Name;
        this.email = email;
        this.faculte = faculte;
        this.password = password;
        this.role_Id = role_Id;
        this.phone = phone;
        this.profilePictureUrl = profilePictureUrl;
        this.bio = bio;
        this.registrationDate = LocalDateTime.now(); // Date automatique
    }

    // Constructeur existant (pour rétrocompatibilité)
    public UserModel(int id_User, String firstName, String last_Name, String email,
                     String faculte, String password, int role_Id) {
        this(id_User, firstName, last_Name, email, faculte, password, role_Id,
                null, null, null, null);
    }

    // Constructeur existant (pour rétrocompatibilité)
    public UserModel(String firstName, String last_Name, String email,
                     String faculte, String password, int role_Id) {
        this(firstName, last_Name, email, faculte, password, role_Id,
                null, null, null);
    }

    // ================= GETTERS & SETTERS =================

    // Getters et Setters existants
    public String getConfirmerPassword() { return confirmerPassword; }
    public void setConfirmerPassword(String confirmerPassword) {
        this.confirmerPassword = confirmerPassword;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getId_User() { return id_User; }
    public void setId_User(int id_User) { this.id_User = id_User; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFaculte() { return faculte; }
    public void setFaculte(String faculte) { this.faculte = faculte; }

    public String getLast_Name() { return last_Name; }
    public void setLast_Name(String last_Name) { this.last_Name = last_Name; }

    public String getFirst_Name() { return first_Name; }
    public void setFirst_Name(String first_Name) { this.first_Name = first_Name; }

    public int getRole_Id() { return role_Id; }
    public void setRole_Id(int role_Id) { this.role_Id = role_Id; }

    public Role getRole() { return role; }
    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            this.roleName = role.getRoleName();
        }
    }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    // ================= NOUVEAUX GETTERS & SETTERS =================

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    // Méthode utilitaire pour formater la date d'inscription
    public String getFormattedRegistrationDate() {
        if (registrationDate == null) return "Non disponible";
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return registrationDate.format(formatter);
    }

    // ================= METHODES EXISTANTES =================

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "id_User=" + id_User +
                ", first_Name='" + first_Name + '\'' +
                ", last_Name='" + last_Name + '\'' +
                ", email='" + email + '\'' +
                ", faculte='" + faculte + '\'' +
                ", role_Id=" + role_Id +
                ", phone='" + phone + '\'' +
                ", registrationDate=" + registrationDate +
                '}';
    }
}