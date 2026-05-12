package com.example.pidev.service.user;

import com.example.pidev.model.user.UserModel;
import com.example.pidev.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {

    private Connection connection;

    public LoginService() throws SQLException {
        connection = DBConnection.getConnection();
    }

    /**
     * Vérifie si les informations de connexion sont valides
     * @param email email de l'utilisateur
     * @param password mot de passe
     * @return UserModel si ok, null sinon
     */
    public UserModel authenticate(String email, String password) {
        String query = "SELECT u.id_user, u.first_name, u.last_name, u.email, u.faculte, u.password, u.role_id " +
                "FROM user_model u " +
                "LEFT JOIN role r ON u.role_id = r.id_role " +
                "WHERE u.email = ? AND u.password = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email.trim());
            stmt.setString(2, password.trim());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Retourne un objet UserModel avec le rôle
                return new UserModel(

                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("faculte"),
                        rs.getString("password"),
                       rs.getInt("role_id")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // échec login
    }
}
