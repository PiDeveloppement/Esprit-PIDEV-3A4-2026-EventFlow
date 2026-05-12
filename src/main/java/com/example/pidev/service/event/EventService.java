package com.example.pidev.service.event;

import com.example.pidev.model.event.Event;
import com.example.pidev.utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les opérations CRUD sur les événements
 * @author Ons Abdesslem
 */
public class EventService {

    private final Connection connection;

    // ==================== CONSTRUCTEUR ====================

    public EventService() {
        this.connection = DBConnection.getConnection();
    }

    // ==================== CREATE ====================

    /**
     * Ajouter un nouvel événement
     * @param event L'événement à ajouter
     * @return true si ajout réussi, false sinon
     */
    public boolean addEvent(Event event) {
        // Validation
        if (!event.isValid()) {
            System.err.println("❌ Erreur: Événement invalide");
            return false;
        }

        String sql = "INSERT INTO event (title, description, start_date, end_date, location, " +
                "capacity, image_url, category_id, created_by, status, is_free, ticket_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, event.getTitle());
            pstmt.setString(2, event.getDescription());
            pstmt.setTimestamp(3, event.getStartDate() != null ? Timestamp.valueOf(event.getStartDate()) : null);
            pstmt.setTimestamp(4, event.getEndDate() != null ? Timestamp.valueOf(event.getEndDate()) : null);
            pstmt.setString(5, event.getLocation());
            pstmt.setInt(6, event.getCapacity());
            pstmt.setString(7, event.getImageUrl());
            pstmt.setInt(8, event.getCategoryId());
            pstmt.setInt(9, event.getCreatedBy());
            pstmt.setString(10, event.getStatus() != null ? event.getStatus().name() : "DRAFT");
            pstmt.setBoolean(11, event.isFree());
            pstmt.setDouble(12, event.getTicketPrice());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                // Récupérer l'ID généré
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        event.setId(rs.getInt(1));
                    }
                }
                System.out.println("✅ Événement ajouté avec succès: " + event.getTitle());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout de l'événement: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== READ ====================

    /**
     * Récupérer tous les événements
     * @return Liste de tous les événements
     */
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event ORDER BY start_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(extractEventFromResultSet(rs));
            }

            System.out.println("✅ " + events.size() + " événements récupérés");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des événements: " + e.getMessage());
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Récupérer un événement par son ID
     * @param id L'ID de l'événement
     * @return L'événement ou null si non trouvé
     */
    public Event getEventById(int id) {
        String sql = "SELECT * FROM event WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractEventFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération de l'événement ID=" + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Récupérer les événements par catégorie
     * @param categoryId L'ID de la catégorie
     * @return Liste des événements de la catégorie
     */
    public List<Event> getEventsByCategory(int categoryId) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event WHERE category_id = ? ORDER BY start_date DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(extractEventFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des événements par catégorie: " + e.getMessage());
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Récupérer les événements par statut
     * @param status Le statut (DRAFT, PUBLISHED)
     * @return Liste des événements avec ce statut
     */
    public List<Event> getEventsByStatus(String status) {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event WHERE status = ? ORDER BY start_date DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, status);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(extractEventFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des événements par statut: " + e.getMessage());
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Récupérer les événements à venir
     * @return Liste des événements futurs
     */
    public List<Event> getUpcomingEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event WHERE start_date > NOW() ORDER BY start_date";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(extractEventFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des événements à venir: " + e.getMessage());
            e.printStackTrace();
        }

        return events;
    }

    // ==================== UPDATE ====================

    /**
     * Mettre à jour un événement
     * @param event L'événement avec les nouvelles valeurs
     * @return true si mise à jour réussie, false sinon
     */
    public boolean updateEvent(Event event) {
        // Validation
        if (event.getId() <= 0) {
            System.err.println("❌ Erreur: ID invalide pour la mise à jour");
            return false;
        }

        if (!event.isValid()) {
            System.err.println("❌ Erreur: Événement invalide");
            return false;
        }

        String sql = "UPDATE event SET title = ?, description = ?, start_date = ?, end_date = ?, " +
                "location = ?, capacity = ?, image_url = ?, category_id = ?, created_by = ?, " +
                "status = ?, is_free = ?, ticket_price = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, event.getTitle());
            pstmt.setString(2, event.getDescription());
            pstmt.setTimestamp(3, event.getStartDate() != null ? Timestamp.valueOf(event.getStartDate()) : null);
            pstmt.setTimestamp(4, event.getEndDate() != null ? Timestamp.valueOf(event.getEndDate()) : null);
            pstmt.setString(5, event.getLocation());
            pstmt.setInt(6, event.getCapacity());
            pstmt.setString(7, event.getImageUrl());
            pstmt.setInt(8, event.getCategoryId());
            pstmt.setInt(9, event.getCreatedBy());
            pstmt.setString(10, event.getStatus() != null ? event.getStatus().name() : "DRAFT");
            pstmt.setBoolean(11, event.isFree());
            pstmt.setDouble(12, event.getTicketPrice());
            pstmt.setInt(13, event.getId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Événement mis à jour avec succès: " + event.getTitle());

                // Recharger l'événement pour obtenir le updated_at mis à jour
                Event updatedEvent = getEventById(event.getId());
                if (updatedEvent != null) {
                    event.setUpdatedAt(updatedEvent.getUpdatedAt());
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour de l'événement: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Publier un événement (changer statut de DRAFT à PUBLISHED)
     * @param id L'ID de l'événement
     * @return true si réussi, false sinon
     */
    public boolean publishEvent(int id) {
        String sql = "UPDATE event SET status = 'PUBLISHED' WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Événement ID=" + id + " publié avec succès");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la publication de l'événement: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== DELETE ====================

    /**
     * Supprimer un événement
     * @param id L'ID de l'événement à supprimer
     * @return true si suppression réussie, false sinon
     */
    public boolean deleteEvent(int id) {
        String sql = "DELETE FROM event WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Événement supprimé avec succès (ID=" + id + ")");
                return true;
            } else {
                System.err.println("⚠️ Aucun événement trouvé avec l'ID " + id);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression de l'événement: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Compter le nombre total d'événements
     * @return Le nombre d'événements
     */
    public int countEvents() {
        String sql = "SELECT COUNT(*) as count FROM event";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des événements: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Vérifier si un titre d'événement existe déjà
     * @param title Le titre à vérifier
     * @param excludeId ID à exclure de la vérification (pour l'update)
     * @return true si le titre existe déjà, false sinon
     */
    public boolean eventTitleExists(String title, int excludeId) {
        String sql = "SELECT COUNT(*) as count FROM event WHERE title = ? AND id != ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setInt(2, excludeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification du titre: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Extraire un événement depuis un ResultSet
     * @param rs Le ResultSet
     * @return L'objet Event
     */
    private Event extractEventFromResultSet(ResultSet rs) throws SQLException {
        Event event = new Event();

        event.setId(rs.getInt("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));

        Timestamp startTimestamp = rs.getTimestamp("start_date");
        if (startTimestamp != null) {
            event.setStartDate(startTimestamp.toLocalDateTime());
        }

        Timestamp endTimestamp = rs.getTimestamp("end_date");
        if (endTimestamp != null) {
            event.setEndDate(endTimestamp.toLocalDateTime());
        }

        event.setLocation(rs.getString("location"));
        event.setGouvernorat(rs.getString("gouvernorat"));
        event.setVille(rs.getString("ville"));
        event.setCapacity(rs.getInt("capacity"));
        event.setImageUrl(rs.getString("image_url"));
        event.setCategoryId(rs.getInt("category_id"));
        event.setCreatedBy(rs.getInt("created_by"));
        event.setStatus(rs.getString("status"));
        event.setFree(rs.getBoolean("is_free"));
        event.setTicketPrice(rs.getDouble("ticket_price"));

        Timestamp createdTimestamp = rs.getTimestamp("created_at");
        if (createdTimestamp != null) {
            event.setCreatedAt(createdTimestamp.toLocalDateTime());
        }

        Timestamp updatedTimestamp = rs.getTimestamp("updated_at");
        if (updatedTimestamp != null) {
            event.setUpdatedAt(updatedTimestamp.toLocalDateTime());
        }

        return event;
    }
    // ==================== MÉTHODES POUR LE CHATBOT ====================

    /**
     * Récupère les événements gratuits
     */
    public List<Event> getFreeEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event WHERE is_free = true ORDER BY start_date";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.add(extractEventFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getFreeEvents: " + e.getMessage());
        }
        return events;
    }

    /**
     * Récupère les événements payants
     */
    public List<Event> getPaidEvents() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event WHERE is_free = false ORDER BY ticket_price DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                events.add(extractEventFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getPaidEvents: " + e.getMessage());
        }
        return events;
    }

    /**
     * Récupère le prochain événement
     */
    public Event getNextEvent() {
        String sql = "SELECT * FROM event WHERE start_date > NOW() ORDER BY start_date LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return extractEventFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNextEvent: " + e.getMessage());
        }
        return null;
    }

    /**
     * Calcule la capacité totale de tous les événements
     */
    public int getTotalCapacity() {
        String sql = "SELECT COALESCE(SUM(capacity), 0) FROM event";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalCapacity: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Récupère le nombre total d'événements
     */
    public int getTotalEventsCount() {
        String sql = "SELECT COUNT(*) FROM event";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getTotalEventsCount: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Récupère le nombre de nouveaux événements ce mois
     */
    public int getNewEventsThisMonth() {
        String sql = "SELECT COUNT(*) FROM event " +
                "WHERE MONTH(created_at) = MONTH(CURRENT_DATE()) " +
                "AND YEAR(created_at) = YEAR(CURRENT_DATE())";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getNewEventsThisMonth: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Calcule le taux de participation moyen
     */
    public double getAverageParticipationRate() {
        String sql = "SELECT AVG(participation_rate) FROM (" +
                "SELECT e.id, (COUNT(t.id) * 100.0 / e.capacity) as participation_rate " +
                "FROM event e LEFT JOIN event_ticket t ON e.id = t.event_id " +
                "GROUP BY e.id, e.capacity" +
                ") as rates";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getAverageParticipationRate: " + e.getMessage());
        }
        return 0.0;
    }
}