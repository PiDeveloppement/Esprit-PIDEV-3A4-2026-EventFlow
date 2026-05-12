package com.example.pidev.service.user;

import com.example.pidev.model.user.PasswordResetToken;
import com.example.pidev.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetService {

    private Connection connection;
    private String currentToken;
    private int currentUserId;

    public PasswordResetService() {
        try {
            this.connection = new DBConnection().getConnection();
            System.out.println("✅ PasswordResetService: Connexion DB établie");
        } catch (Exception e) {
            System.err.println("❌ PasswordResetService: Erreur de connexion - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== GESTION DES TOKENS ==========

    /**
     * Crée un nouveau token de réinitialisation
     */
    public void createToken(PasswordResetToken token) {
        if (connection == null) {
            System.err.println("❌ createToken: Pas de connexion DB");
            return;
        }

        // Vérifier si la table existe, sinon la créer
        createTokenTableIfNotExists();

        // ✅ MODIFIER: Enlever 'created_at' de la requête
        String query = "INSERT INTO password_reset_tokens (token, user_id, expiry_date, used) " +
                "VALUES (?, ?, ?, FALSE)";  // Plus de created_at

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token.getToken());
            stmt.setInt(2, token.getUserId());
            stmt.setTimestamp(3, Timestamp.valueOf(token.getExpiryDate()));

            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("✅ Token créé avec succès pour l'utilisateur ID: " + token.getUserId());
                System.out.println("   Token: " + token.getToken());
                System.out.println("   Expire le: " + token.getExpiryDate());
            } else {
                System.err.println("❌ Échec de création du token");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur createToken: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée la table des tokens si elle n'existe pas
     */
    private void createTokenTableIfNotExists() {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "token VARCHAR(255) NOT NULL UNIQUE, " +
                "user_id INT NOT NULL, " +
                "expiry_date DATETIME NOT NULL, " +
                "used BOOLEAN DEFAULT FALSE, " +
                "created_at DATETIME DEFAULT NOW(), " +
                "INDEX idx_token (token), " +
                "INDEX idx_user_id (user_id)" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableQuery);
            System.out.println("✅ Table password_reset_tokens vérifiée/créée");
        } catch (SQLException e) {
            System.err.println("❌ Erreur création table: " + e.getMessage());
        }
    }

    /**
     * Valide un token
     */
    public boolean validateToken(String token) {
        if (connection == null) {
            System.err.println("❌ validateToken: Pas de connexion DB");
            return false;
        }

        if (token == null || token.trim().isEmpty()) {
            System.err.println("❌ validateToken: Token vide");
            return false;
        }

        String query = "SELECT user_id FROM password_reset_tokens " +
                "WHERE token = ? AND used = FALSE AND expiry_date > NOW()";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentToken = token;
                currentUserId = rs.getInt("user_id");
                System.out.println("✅ Token valide pour l'utilisateur ID: " + currentUserId);
                return true;
            } else {
                System.out.println("❌ Token invalide ou expiré: " + token);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur validateToken: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marque un token comme utilisé
     */
    private void markTokenAsUsed(String token) {
        if (connection == null) return;

        String query = "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("✅ Token marqué comme utilisé: " + token);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur markTokenAsUsed: " + e.getMessage());
        }
    }

    /**
     * Nettoie les tokens expirés
     */
    public void cleanExpiredTokens() {
        if (connection == null) return;

        String query = "DELETE FROM password_reset_tokens WHERE expiry_date < NOW() OR used = TRUE";

        try (Statement stmt = connection.createStatement()) {
            int rowsDeleted = stmt.executeUpdate(query);
            if (rowsDeleted > 0) {
                System.out.println("🧹 Nettoyage: " + rowsDeleted + " tokens expirés supprimés");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur cleanExpiredTokens: " + e.getMessage());
        }
    }

    // ========== GESTION DES MOTS DE PASSE ==========

    /**
     * Réinitialise le mot de passe (SANS HASHAGE - en clair)
     * ⚠️ ATTENTION: Non sécurisé, à utiliser uniquement pour le développement
     */
    public boolean resetPassword(String newPassword) {
        if (connection == null) {
            System.err.println("❌ resetPassword: Pas de connexion DB");
            return false;
        }

        if (currentToken == null || currentUserId == 0) {
            System.err.println("❌ resetPassword: Aucun token valide en session");
            return false;
        }

        // Vérifier une dernière fois que le token est toujours valide
        if (!validateToken(currentToken)) {
            System.err.println("❌ resetPassword: Token expiré ou déjà utilisé");
            return false;
        }

        // ⚠️ STOCKAGE EN CLAIR - NON SÉCURISÉ
        String updateQuery = "UPDATE user_model SET password = ? WHERE Id_User = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
            stmt.setString(1, newPassword);  // Stockage en clair !
            stmt.setInt(2, currentUserId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Marquer le token comme utilisé
                markTokenAsUsed(currentToken);

                System.out.println("⚠️ ATTENTION: Mot de passe stocké EN CLAIR pour l'utilisateur ID: " + currentUserId);
                System.out.println("✅ Mot de passe réinitialisé avec succès");

                // Réinitialiser les variables de session
                currentToken = null;
                currentUserId = 0;

                return true;
            } else {
                System.err.println("❌ Aucun utilisateur trouvé avec l'ID: " + currentUserId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur resetPassword: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Version alternative avec hashage (recommandée pour la production)
     * Décommentez cette méthode si vous voulez utiliser le hashage
     */
    /*
    public boolean resetPasswordWithHash(String newPassword) {
        if (connection == null) return false;
        if (currentToken == null || currentUserId == 0) return false;

        if (!validateToken(currentToken)) return false;

        // Hasher le mot de passe
        String hashedPassword = hashPassword(newPassword);

        String updateQuery = "UPDATE user_model SET password = ? WHERE Id_User = ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, currentUserId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                markTokenAsUsed(currentToken);
                System.out.println("✅ Mot de passe hashé et réinitialisé pour l'utilisateur ID: " + currentUserId);
                currentToken = null;
                currentUserId = 0;
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }
    */

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Génère un token unique
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Récupère l'ID utilisateur associé à un token
     */
    public int getUserIdFromToken(String token) {
        if (connection == null) return -1;

        String query = "SELECT user_id FROM password_reset_tokens WHERE token = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Vérifie si un token existe
     */
    public boolean tokenExists(String token) {
        if (connection == null) return false;

        String query = "SELECT COUNT(*) FROM password_reset_tokens WHERE token = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Supprime un token
     */
    public boolean deleteToken(String token) {
        if (connection == null) return false;

        String query = "DELETE FROM password_reset_tokens WHERE token = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, token);
            int rowsDeleted = stmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère le nombre de tokens actifs pour un utilisateur
     */
    public int getActiveTokensCount(int userId) {
        if (connection == null) return 0;

        String query = "SELECT COUNT(*) FROM password_reset_tokens " +
                "WHERE user_id = ? AND used = FALSE AND expiry_date > NOW()";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ========== GETTERS/SETTERS ==========

    public String getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(String token) {
        this.currentToken = token;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }

    /**
     * Ferme la connexion
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Connexion DB fermée");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}