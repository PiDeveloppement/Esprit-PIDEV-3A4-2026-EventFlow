package com.example.pidev.service.user;

import com.example.pidev.model.role.Role;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// Add import at the top of the file
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    public Connection connection;

    public UserService() {
        try {
            this.connection = new DBConnection().getConnection();
        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion à la base de données: " + e.getMessage());
            e.printStackTrace();
            this.connection = null;
        }
    }

    // 🔹 Créer un utilisateur avec gestion d'exceptions
    // Dans UserService.java, modifiez la méthode registerUser

    // Dans UserService.java, méthode registerUser
    public boolean registerUser(UserModel user) {
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return false;
        }

        String query = "INSERT INTO user_model(First_Name, Last_Name, Email, Faculte, Password, Role_Id, phone, profile_picture_url, registration_date, bio) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getFirst_Name());
            stmt.setString(2, user.getLast_Name());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getFaculte());
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(13));
            stmt.setString(5, hashedPassword);
            stmt.setInt(6, 1); // Role_Id par défaut

            if (user.getPhone() != null) {
                stmt.setString(7, user.getPhone());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            if (user.getProfilePictureUrl() != null) {
                stmt.setString(8, user.getProfilePictureUrl());
            } else {
                stmt.setNull(8, Types.VARCHAR);
            }

            if (user.getRegistrationDate() != null) {
                stmt.setTimestamp(9, Timestamp.valueOf(user.getRegistrationDate()));
            } else {
                stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            }

            if (user.getBio() != null) {
                stmt.setString(10, user.getBio());
            } else {
                stmt.setNull(10, Types.VARCHAR);
            }

            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("✅ Utilisateur inscrit: " + user.getEmail());

                // ✅ ENVOYER L'EMAIL DE BIENVENUE DANS UN THREAD SÉPARÉ
                new Thread(() -> {
                    try {
                        EmailService.sendWelcomeEmail(user.getEmail(), user.getFirst_Name());
                    } catch (Exception e) {
                        System.err.println("⚠️ Erreur envoi email (non bloquant): " + e.getMessage());
                    }
                }).start();

                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'inscription: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(long id) {
        if (connection == null) return false;

        try {
            connection.setAutoCommit(false);
            System.out.println("🔄 Suppression utilisateur ID: " + id);

            // Étape 1: Supprimer les tokens
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM password_reset_tokens WHERE user_id = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Tokens: " + e.getMessage()); }

            // Étape 2: Supprimer les tickets
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM event_ticket WHERE user_id = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Tickets: " + e.getMessage()); }

            // Étape 3: Supprimer les feedbacks
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM feedbacks WHERE id_user = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Feedbacks: " + e.getMessage()); }

            // Étape 4: Supprimer les notifications
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM notification WHERE user_id = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Notifications: " + e.getMessage()); }

            // Étape 5: Supprimer les questions
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM questions WHERE id_user = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Questions: " + e.getMessage()); }

            // Étape 6: Supprimer les quiz sessions
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM quiz_sessions WHERE user_id = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Quiz sessions: " + e.getMessage()); }

            // Étape 7: Supprimer les sponsors
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM sponsor WHERE user_id = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Sponsor: " + e.getMessage()); }

            // Étape 8: Nullifier les événements créés par l'utilisateur
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE event SET created_by = NULL WHERE created_by = ?")) {
                stmt.setLong(1, id); stmt.executeUpdate();
            } catch (SQLException e) { System.out.println("⚠️ Events: " + e.getMessage()); }

            // Étape 9: Supprimer l'utilisateur
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM user_model WHERE Id_User = ?")) {
                stmt.setLong(1, id);
                int userRows = stmt.executeUpdate();
                if (userRows > 0) {
                    connection.commit();
                    System.out.println("✅ Utilisateur " + id + " supprimé");
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) {}
        }
    }
    // 🔹 Récupérer tous les utilisateurs
    public ObservableList<UserModel> getAllUsers() {
        ObservableList<UserModel> users = FXCollections.observableArrayList();

        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return users;
        }

        String query = "SELECT u.*, r.RoleName " +
                "FROM user_model u " +
                "LEFT JOIN role r ON u.Role_Id = r.Id_Role";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Créer et remplir l'objet Role
                Role role = new Role();
                role.setId_Role(rs.getInt("Role_Id"));
                role.setRoleName(rs.getString("RoleName"));

                // Créer l'utilisateur avec les champs de base
                UserModel user = new UserModel(
                        rs.getInt("Id_User"),
                        rs.getString("First_Name"),
                        rs.getString("Last_Name"),
                        rs.getString("Email"),
                        rs.getString("Faculte"),
                        rs.getString("Password"),
                        rs.getInt("Role_Id")
                );

                // Ajouter les nouveaux champs
                user.setPhone(rs.getString("phone"));
                user.setProfilePictureUrl(rs.getString("profile_picture_url"));

                // Récupérer la date d'inscription (peut être null)
                Timestamp regDate = rs.getTimestamp("registration_date");
                if (regDate != null) {
                    user.setRegistrationDate(regDate.toLocalDateTime());
                }

                user.setBio(rs.getString("bio"));
                user.setRole(role); // Associer le rôle à l'utilisateur

                users.add(user);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // 🔹 Modifier un utilisateur
    public boolean updateUser(UserModel user) {
        if (connection == null) return false;

        String query = "UPDATE user_model SET " +
                "First_Name=?, Last_Name=?, Email=?, Faculte=?, Password=?, Role_Id=?, " +
                "phone=?, profile_picture_url=?, bio=? " +
                "WHERE Id_User=?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, user.getFirst_Name());
            stmt.setString(2, user.getLast_Name());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getFaculte());
            stmt.setString(5, user.getPassword());
            stmt.setInt(6, user.getRole_Id());

            // Nouveaux champs (gestion des valeurs null)
            if (user.getPhone() != null) {
                stmt.setString(7, user.getPhone());
            } else {
                stmt.setNull(7, Types.VARCHAR);
            }

            if (user.getProfilePictureUrl() != null) {
                stmt.setString(8, user.getProfilePictureUrl());
            } else {
                stmt.setNull(8, Types.VARCHAR);
            }

            if (user.getBio() != null) {
                stmt.setString(9, user.getBio());
            } else {
                stmt.setNull(9, Types.VARCHAR);
            }

            stmt.setLong(10, user.getId_User());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Récupérer un utilisateur par email
    public UserModel getUserByEmail(String email) {
        if (connection == null) return null;

        String query = "SELECT u.*, r.RoleName FROM user_model u " +
                "LEFT JOIN role r ON u.Role_Id = r.Id_Role WHERE u.Email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Role role = new Role();
                role.setId_Role(rs.getInt("Role_Id"));
                role.setRoleName(rs.getString("RoleName"));

                UserModel user = new UserModel(
                        rs.getInt("Id_User"),
                        rs.getString("First_Name"),
                        rs.getString("Last_Name"),
                        rs.getString("Email"),
                        rs.getString("Faculte"),
                        rs.getString("Password"),
                        rs.getInt("Role_Id")
                );

                user.setPhone(rs.getString("phone"));
                user.setProfilePictureUrl(rs.getString("profile_picture_url"));

                Timestamp regDate = rs.getTimestamp("registration_date");
                if (regDate != null) {
                    user.setRegistrationDate(regDate.toLocalDateTime());
                }

                user.setBio(rs.getString("bio"));
                user.setRole(role);

                return user;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getUserByEmail: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ==================== NOUVELLES MÉTHODES POUR TÉLÉPHONE ====================

    /**
     * 🔹 Récupérer un utilisateur par numéro de téléphone
     * @param phoneNumber Le numéro de téléphone à rechercher
     * @return UserModel ou null si non trouvé
     */
    public UserModel getUserByPhone(String phoneNumber) {
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return null;
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            System.err.println("❌ Numéro de téléphone vide");
            return null;
        }

        // Nettoyer le numéro pour la recherche
        String cleanPhone = normalizePhoneNumber(phoneNumber);

        String query = "SELECT u.*, r.RoleName FROM user_model u " +
                "LEFT JOIN role r ON u.Role_Id = r.Id_Role " +
                "WHERE u.phone = ? OR REPLACE(u.phone, '+', '') = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            // Recherche avec et sans le +
            String phoneWithPlus = cleanPhone.startsWith("+") ? cleanPhone : "+" + cleanPhone;
            String phoneWithoutPlus = cleanPhone.replace("+", "");

            stmt.setString(1, phoneWithPlus);
            stmt.setString(2, phoneWithoutPlus);

            System.out.println("🔍 Recherche téléphone - Avec +: " + phoneWithPlus + ", Sans +: " + phoneWithoutPlus);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            } else {
                System.out.println("ℹ️ Aucun utilisateur trouvé avec le numéro: " + phoneNumber);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur getUserByPhone: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 🔹 Vérifier si un numéro de téléphone existe déjà
     * @param phoneNumber Le numéro à vérifier
     * @return true si le numéro existe
     */
    public boolean isPhoneNumberExists(String phoneNumber) {
        if (connection == null) return false;

        String cleanPhone = normalizePhoneNumber(phoneNumber);
        String phoneWithPlus = cleanPhone.startsWith("+") ? cleanPhone : "+" + cleanPhone;
        String phoneWithoutPlus = cleanPhone.replace("+", "");

        String query = "SELECT COUNT(*) FROM user_model WHERE phone = ? OR REPLACE(phone, '+', '') = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, phoneWithPlus);
            ps.setString(2, phoneWithoutPlus);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur isPhoneNumberExists: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 🔹 Mettre à jour le numéro de téléphone d'un utilisateur
     * @param userId L'ID de l'utilisateur
     * @param phoneNumber Le nouveau numéro
     * @return true si succès
     */
    public boolean updateUserPhone(int userId, String phoneNumber) {
        if (connection == null) return false;

        String cleanPhone = normalizePhoneNumber(phoneNumber);

        String query = "UPDATE user_model SET phone = ? WHERE Id_User = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, cleanPhone);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Numéro de téléphone mis à jour pour l'utilisateur ID: " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur updateUserPhone: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 🔹 Récupérer tous les utilisateurs qui ont un numéro de téléphone
     * @return Liste des utilisateurs avec téléphone
     */
    public ObservableList<UserModel> getUsersWithPhone() {
        ObservableList<UserModel> users = FXCollections.observableArrayList();

        if (connection == null) return users;

        String query = "SELECT u.*, r.RoleName FROM user_model u " +
                "LEFT JOIN role r ON u.Role_Id = r.Id_Role " +
                "WHERE u.phone IS NOT NULL AND u.phone != ''";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            System.out.println("📱 " + users.size() + " utilisateurs avec téléphone trouvés");

        } catch (SQLException e) {
            System.err.println("❌ Erreur getUsersWithPhone: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Normalise un numéro de téléphone
     * @param phoneNumber Le numéro à normaliser
     * @return Le numéro normalisé
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        // Enlever tous les caractères non numériques sauf le +
        String normalized = phoneNumber.replaceAll("[^0-9+]", "");

        // Si le numéro commence par 0 et fait 8 chiffres (format tunisien local)
        if (normalized.startsWith("0") && normalized.length() == 8) {
            normalized = "+216" + normalized.substring(1);
        }
        // Si le numéro commence par 216 (sans +)
        else if (normalized.startsWith("216") && normalized.length() == 11) {
            normalized = "+" + normalized;
        }
        // Si le numéro fait 8 chiffres (numéro tunisien sans indicatif)
        else if (normalized.length() == 8 && !normalized.startsWith("0")) {
            normalized = "+216" + normalized;
        }

        return normalized;
    }

    /**
     * Mappe un ResultSet vers un objet UserModel
     * @param rs Le ResultSet
     * @return UserModel
     * @throws SQLException
     */
    private UserModel mapResultSetToUser(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId_Role(rs.getInt("Role_Id"));
        role.setRoleName(rs.getString("RoleName"));

        UserModel user = new UserModel(
                rs.getInt("Id_User"),
                rs.getString("First_Name"),
                rs.getString("Last_Name"),
                rs.getString("Email"),
                rs.getString("Faculte"),
                rs.getString("Password"),
                rs.getInt("Role_Id")
        );

        user.setPhone(rs.getString("phone"));
        user.setProfilePictureUrl(rs.getString("profile_picture_url"));

        Timestamp regDate = rs.getTimestamp("registration_date");
        if (regDate != null) {
            user.setRegistrationDate(regDate.toLocalDateTime());
        }

        user.setBio(rs.getString("bio"));
        user.setRole(role);

        return user;
    }

    // 🔹 Mettre à jour le mot de passe
    public boolean updateUserPassword(int userId, String newPassword) {
        if (connection == null) return false;

        String query = "UPDATE user_model SET password = ? WHERE Id_User = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newPassword);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            System.out.println("✅ Mot de passe mis à jour pour l'utilisateur ID: " + userId);
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("❌ Erreur updateUserPassword: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Récupérer un utilisateur par ID
    public UserModel getUserById(int id) {
        if (connection == null) return null;

        String query = "SELECT u.*, r.RoleName FROM user_model u " +
                "LEFT JOIN role r ON u.Role_Id = r.Id_Role WHERE u.Id_User=?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getUserById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 Authentification
    public UserModel authenticate(String email, String password) {
        if (connection == null) return null;

        String query = "SELECT u.*, r.RoleName FROM user_model u " +
                "LEFT JOIN role r ON u.Role_Id = r.Id_Role " +
                "WHERE u.Email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("Password");
                boolean passwordMatches = false;

                // Check if it's a bcrypt hash
                if (storedPassword != null && storedPassword.startsWith("$2")) {
                    try {
                        // Fix PHP bcrypt format ($2y$) to Java bcrypt format ($2a$)
                        String compatibleHash = storedPassword.replace("$2y$", "$2a$");
                        passwordMatches = BCrypt.checkpw(password, compatibleHash);
                    } catch (IllegalArgumentException e) {
                        System.err.println("⚠️ Invalid bcrypt hash for: " + email);
                        passwordMatches = password.equals(storedPassword);
                    }
                } else {
                    // Plain text — direct comparison (old users)
                    passwordMatches = password.equals(storedPassword);
                }

                if (passwordMatches) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur authenticate: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 Récupérer toutes les facultés
    public ObservableList<String> getAllFacultes() {
        ObservableList<String> faculteList = FXCollections.observableArrayList();

        if (connection == null) return faculteList;

        String query = "SELECT DISTINCT faculte FROM user_model WHERE faculte IS NOT NULL AND faculte != '' ORDER BY faculte";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                faculteList.add(rs.getString("faculte"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getAllFacultes: " + e.getMessage());
            e.printStackTrace();
        }
        return faculteList;
    }

    // 🔹 Vérifier si email existe
    public boolean isEmailExists(String email) {
        if (connection == null) return false;

        String query = "SELECT COUNT(*) FROM user_model WHERE Email = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur isEmailExists: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 🔹 Compter les participants
    public int getTotalParticipantsCount() {
        if (connection == null) return 0;

        String query = "SELECT COUNT(*) FROM user_model";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalParticipantsCount: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    // Dans UserService.java, ajoutez ces méthodes :

    /**
     * Récupère tous les emails des administrateurs
     */
    public List<String> getAllAdminEmails() {
        List<String> adminEmails = new ArrayList<>();

        if (connection == null) return adminEmails;

        // Version avec paramètre pour plus de sécurité
        String query = "SELECT u.Email FROM user_model u " +
                "INNER JOIN role r ON u.Role_Id = r.Id_Role " +
                "WHERE LOWER(r.RoleName) = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "admin");

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                adminEmails.add(rs.getString("Email"));
            }

            System.out.println("📧 " + adminEmails.size() + " admin(s) trouvé(s)");

            // Si aucun admin trouvé, utilisez votre email par défaut
            if (adminEmails.isEmpty()) {
                System.out.println("⚠️ Aucun admin trouvé - Utilisation de l'email par défaut");
                adminEmails.add("sellamiarij7@gmail.com"); // Votre email
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        return adminEmails;
    }
    public int getNewUsersThisMonthCount() {
        if (connection == null) return 0;

        String query = "SELECT COUNT(*) FROM user_model " +
                "WHERE registration_date IS NOT NULL " +
                "AND MONTH(registration_date) = MONTH(CURRENT_DATE()) " +
                "AND YEAR(registration_date) = YEAR(CURRENT_DATE())";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNewUsersThisMonthCount: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    // ==================== MÉTHODES POUR LE CHATBOT ====================

    /**
     * Récupère le nombre total d'utilisateurs
     */
    public int getTotalUsersCount() {
        if (connection == null) return 0;

        String sql = "SELECT COUNT(*) FROM user_model";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalUsersCount: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Récupère les utilisateurs par rôle (sans utiliser la jointure directe)
     */
    public List<UserModel> getUsersByRole(String roleName) {
        List<UserModel> users = new ArrayList<>();
        if (connection == null) return users;

        // Récupérer d'abord l'ID du rôle
        String roleQuery = "SELECT Id_Role FROM role WHERE LOWER(RoleName) LIKE LOWER(?)";
        try (PreparedStatement roleStmt = connection.prepareStatement(roleQuery)) {
            roleStmt.setString(1, "%" + roleName + "%");
            ResultSet roleRs = roleStmt.executeQuery();

            if (roleRs.next()) {
                int roleId = roleRs.getInt("Id_Role");

                // Puis récupérer les utilisateurs avec cet ID
                String userQuery = "SELECT * FROM user_model WHERE Role_Id = ?";
                try (PreparedStatement userStmt = connection.prepareStatement(userQuery)) {
                    userStmt.setInt(1, roleId);
                    ResultSet userRs = userStmt.executeQuery();

                    while (userRs.next()) {
                        UserModel user = mapResultSetToUser(userRs);
                        users.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getUsersByRole: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Récupère le nombre de nouveaux utilisateurs ce mois
     */
    public int getNewUsersThisMonth() {
        return getNewUsersThisMonthCount(); // Utilise la méthode existante
    }

    /**
     * Récupère les statistiques des utilisateurs par rôle
     */
    public List<Object[]> getUsersCountByRole() {
        List<Object[]> stats = new ArrayList<>();
        if (connection == null) return stats;

        String sql = "SELECT r.RoleName, COUNT(u.Id_User) as count " +
                "FROM role r LEFT JOIN user_model u ON r.Id_Role = u.Role_Id " +
                "GROUP BY r.Id_Role, r.RoleName";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.add(new Object[]{
                        rs.getString("RoleName"),
                        rs.getInt("count")
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getUsersCountByRole: " + e.getMessage());
            e.printStackTrace();
        }
        return stats;
    }
}
