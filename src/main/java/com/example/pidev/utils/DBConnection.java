package com.example.pidev.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Configuration de votre base de données pidev
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/pidev";
    private static final String USER = "root";
    private static final String PASSWORD = "souhail123";

    private static Connection connection = null;
    private static DBConnection instance;

    static {
        try {
            // Charger le driver MySQL (optionnel pour JDBC 4+)
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ Driver MySQL chargé");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver MySQL non trouvé: " + e.getMessage());
        }
    }

    // Constructeur privé pour le pattern Singleton
    public DBConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base de données pidev établie");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtient l'instance unique de DBConnection (Singleton)
     * @return L'instance de DBConnection
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Obtient la connexion à la base de données (méthode statique)
     * @return L'objet Connection
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à la base de données établie");
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Alias pour getConnection() (compatible avec l'ancien code)
     * @return L'objet Connection
     */
    public Connection getCnx() {
        return getConnection();
    }

    /**
     * Méthode statique pour obtenir directement la connexion (compatible avec l'ancien code)
     * @return L'objet Connection
     */
    public static Connection getConnectionInstance() {
        return getConnection();
    }

    /**
     * Ferme la connexion à la base de données
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✅ Connexion fermée");
            } catch (SQLException e) {
                System.err.println("❌ Erreur lors de la fermeture de la connexion: " + e.getMessage());
            }
        }
    }

    /**
     * Vérifie si la connexion est valide
     * @return true si la connexion est valide, false sinon
     */
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}