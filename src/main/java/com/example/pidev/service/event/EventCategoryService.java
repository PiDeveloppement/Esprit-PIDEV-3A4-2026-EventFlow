package com.example.pidev.service.event;

import com.example.pidev.model.event.EventCategory;
import com.example.pidev.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les opérations CRUD sur les catégories d'événements
 *
 * @author Ons Abdesslem (adapté par vous)
 * @version 1.0
 */
public class EventCategoryService {

    private Connection connection;

    // ==================== CONSTRUCTEUR ====================

    public EventCategoryService() {
        // Initialiser la connexion
        this.connection = DBConnection.getConnection();
        if (this.connection == null) {
            System.err.println("❌ Erreur de connexion à la base de données");
        } else {
            System.out.println("✅ Connexion établie pour EventCategoryService");
        }
    }

    // ==================== CREATE ====================

    /**
     * Ajouter une nouvelle catégorie
     * @param category La catégorie à ajouter
     * @return true si ajout réussi, false sinon
     */
    public boolean addCategory(EventCategory category) {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return false;
        }

        // Validation
        if (!category.isValid()) {
            System.err.println("Erreur: Catégorie invalide (nom vide ou trop long)");
            return false;
        }

        // Vérifier que le nom n'existe pas déjà
        if (categoryNameExists(category.getName(), 0)) {
            System.err.println("Erreur: Une catégorie avec le nom '" + category.getName() + "' existe déjà");
            return false;
        }

        String sql = "INSERT INTO event_category (name, description, icon, color, is_active) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setString(3, category.getIcon());
            pstmt.setString(4, category.getColor());
            pstmt.setBoolean(5, category.isActive());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                // Récupérer l'ID généré
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        category.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Catégorie ajoutée avec succès: " + category.getName());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout de la catégorie: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    // ==================== READ ====================

    /**
     * Récupérer toutes les catégories (sans compter les événements)
     * @return Liste de toutes les catégories
     */
    public List<EventCategory> getAllCategories() {
        List<EventCategory> categories = new ArrayList<>();

        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return categories;
        }

        String sql = "SELECT * FROM event_category ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(extractCategoryFromResultSet(rs, false));
            }

            System.out.println("✅ " + categories.size() + " catégories récupérées");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des catégories: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    /**
     * Récupérer toutes les catégories AVEC le nombre d'événements (optimisé)
     * @return Liste de toutes les catégories avec eventCount
     */
    public List<EventCategory> getAllCategoriesWithCount() {
        List<EventCategory> categories = new ArrayList<>();

        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return categories;
        }

        // Requête JOIN optimisée pour récupérer tout en une seule fois
        String sql = "SELECT ec.*, COUNT(e.id) as event_count " +
                "FROM event_category ec " +
                "LEFT JOIN event e ON ec.id = e.category_id " +
                "GROUP BY ec.id " +
                "ORDER BY ec.name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EventCategory category = extractCategoryFromResultSet(rs, false);
                category.setEventCount(rs.getInt("event_count"));
                categories.add(category);
            }

            System.out.println("✅ " + categories.size() + " catégories récupérées avec comptage");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des catégories: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    /**
     * Récupérer une catégorie par son ID
     * @param id L'ID de la catégorie
     * @return La catégorie ou null si non trouvée
     */
    public EventCategory getCategoryById(int id) {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return null;
        }

        String sql = "SELECT * FROM event_category WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    EventCategory category = extractCategoryFromResultSet(rs, false);
                    // Compter les événements pour cette catégorie
                    category.setEventCount(countEventsByCategory(id));
                    return category;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération de la catégorie ID=" + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Récupérer uniquement les catégories actives
     * @return Liste des catégories actives
     */
    public List<EventCategory> getActiveCategories() {
        List<EventCategory> categories = new ArrayList<>();

        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return categories;
        }

        String sql = "SELECT * FROM event_category WHERE is_active = 1 ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categories.add(extractCategoryFromResultSet(rs, false));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des catégories actives: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }


    // ==================== UPDATE ====================

    /**
     * Mettre à jour une catégorie
     * @param category La catégorie avec les nouvelles valeurs
     * @return true si mise à jour réussie, false sinon
     */
    public boolean updateCategory(EventCategory category) {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return false;
        }

        // Validation
        if (category.getId() <= 0) {
            System.err.println("Erreur: ID invalide pour la mise à jour");
            return false;
        }

        if (!category.isValid()) {
            System.err.println("Erreur: Catégorie invalide (nom vide ou trop long)");
            return false;
        }

        // Vérifier que le nom n'existe pas déjà (sauf pour cette catégorie)
        if (categoryNameExists(category.getName(), category.getId())) {
            System.err.println("Erreur: Une autre catégorie avec le nom '" + category.getName() + "' existe déjà");
            return false;
        }

        String sql = "UPDATE event_category SET name = ?, description = ?, icon = ?, color = ?, is_active = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setString(3, category.getIcon());
            pstmt.setString(4, category.getColor());
            pstmt.setBoolean(5, category.isActive());
            pstmt.setInt(6, category.getId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Catégorie mise à jour avec succès: " + category.getName());

                // ========== IMPORTANT ==========
                // Recharger la catégorie depuis la base de données pour obtenir le updated_at mis à jour
                System.out.println("📝 Rechargement de la catégorie pour récupérer updated_at...");
                EventCategory updatedCategory = getCategoryById(category.getId());
                if (updatedCategory != null) {
                    // Mettre à jour l'objet passé en paramètre avec les nouvelles valeurs
                    category.setUpdatedAt(updatedCategory.getUpdatedAt());
                    System.out.println("✅ updated_at mis à jour: " + updatedCategory.getUpdatedAt());
                } else {
                    System.err.println("⚠️ Impossible de recharger la catégorie");
                }

                return true;
            } else {
                System.err.println("⚠️ Aucune catégorie trouvée avec l'ID " + category.getId());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour de la catégorie: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    // ==================== DELETE ====================

    /**
     * Supprimer une catégorie
     * @param id L'ID de la catégorie à supprimer
     * @return true si suppression réussie, false sinon
     */
    public boolean deleteCategory(int id) {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return false;
        }

        // Vérifier si la catégorie est utilisée par des événements
        if (isCategoryUsed(id)) {
            System.err.println("❌ Impossible de supprimer: Cette catégorie est utilisée par des événements");
            return false;
        }

        String sql = "DELETE FROM event_category WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Catégorie supprimée avec succès (ID=" + id + ")");
                return true;
            } else {
                System.err.println("⚠️ Aucune catégorie trouvée avec l'ID " + id);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression de la catégorie: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Compter le nombre d'événements associés à une catégorie
     * @param categoryId L'ID de la catégorie
     * @return Le nombre d'événements
     */
    public int countEventsByCategory(int categoryId) {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM event WHERE category_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des événements: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Compter le nombre total de catégories
     * @return Le nombre de catégories
     */
    public int countCategories() {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM event_category";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des catégories: " + e.getMessage());
        }

        return 0;
    }


    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Vérifier si un nom de catégorie existe déjà
     * @param name Le nom à vérifier
     * @param excludeId ID à exclure de la vérification (pour l'update)
     * @return true si le nom existe déjà, false sinon
     */
    private boolean categoryNameExists(String name, int excludeId) {
        // Vérifier la connexion
        if (connection == null) {
            System.err.println("❌ Pas de connexion à la base de données");
            return false;
        }

        String sql = "SELECT COUNT(*) as count FROM event_category WHERE name = ? AND id != ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, excludeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification du nom: " + e.getMessage());
        }

        return false;
    }

    /**
     * Vérifier si une catégorie est utilisée par des événements
     * @param categoryId L'ID de la catégorie
     * @return true si utilisée, false sinon
     */
    private boolean isCategoryUsed(int categoryId) {
        return countEventsByCategory(categoryId) > 0;
    }

    /**
     * Extraire une catégorie depuis un ResultSet
     * @param rs Le ResultSet
     * @param includeCount Si true, compte les événements (attention: requête supplémentaire)
     * @return L'objet EventCategory
     */
    private EventCategory extractCategoryFromResultSet(ResultSet rs, boolean includeCount) throws SQLException {
        EventCategory category = new EventCategory();

        category.setId(rs.getInt("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setIcon(rs.getString("icon"));
        category.setColor(rs.getString("color"));
        category.setActive(rs.getBoolean("is_active"));

        // Conversion Timestamp -> LocalDateTime
        Timestamp createdTimestamp = rs.getTimestamp("created_at");
        if (createdTimestamp != null) {
            category.setCreatedAt(createdTimestamp.toLocalDateTime());
        }

        Timestamp updatedTimestamp = rs.getTimestamp("updated_at");
        if (updatedTimestamp != null) {
            category.setUpdatedAt(updatedTimestamp.toLocalDateTime());
        }

        // Compter les événements seulement si demandé
        if (includeCount) {
            category.setEventCount(countEventsByCategory(category.getId()));
        }

        return category;
    }
}