package com.example.pidev.service.event;

import com.example.pidev.model.event.Event;
import com.example.pidev.utils.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

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

        try {
            Set<String> writableColumns = loadWritableEventColumns();
            Map<String, Object> values = buildEventWriteMap(event, writableColumns);

            if (values.isEmpty()) {
                System.err.println("❌ Erreur: aucune colonne écrivable détectée dans la table event");
                return false;
            }

            String columns = String.join(", ", values.keySet());
            String placeholders = String.join(", ", values.keySet().stream().map(k -> "?").toList());
            String sql = "INSERT INTO event (" + columns + ") VALUES (" + placeholders + ")";

            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int index = 1;
                for (Object value : values.values()) {
                    bindValue(pstmt, index++, value);
                }

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

        try {
            Set<String> writableColumns = loadWritableEventColumns();
            Map<String, Object> values = buildEventWriteMap(event, writableColumns);

            if (values.isEmpty()) {
                System.err.println("❌ Erreur: aucune colonne écrivable détectée pour la mise à jour");
                return false;
            }

            String assignments = String.join(", ", values.keySet().stream().map(k -> k + " = ?").toList());
            String sql = "UPDATE event SET " + assignments + " WHERE id = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int index = 1;
                for (Object value : values.values()) {
                    bindValue(pstmt, index++, value);
                }
                pstmt.setInt(index, event.getId());

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

    private Set<String> loadWritableEventColumns() throws SQLException {
        Set<String> columns = new HashSet<>();
        String sql = """
                SELECT COLUMN_NAME, EXTRA
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'event'
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                String extra = rs.getString("EXTRA");
                boolean generated = extra != null && extra.toLowerCase().contains("generated");
                if (!generated && column != null) {
                    columns.add(column.toLowerCase());
                }
            }
        }
        return columns;
    }

    private Map<String, Object> buildEventWriteMap(Event event, Set<String> writableColumns) {
        Map<String, Object> values = new LinkedHashMap<>();

        putIfWritable(values, writableColumns, "title", event.getTitle());
        putIfWritable(values, writableColumns, "description", event.getDescription());
        putIfWritable(values, writableColumns, "location", event.getLocation());
        putIfWritable(values, writableColumns, "gouvernorat", event.getGouvernorat());
        putIfWritable(values, writableColumns, "ville", event.getVille());
        putIfWritable(values, writableColumns, "capacity", event.getCapacity());
        putIfWritable(values, writableColumns, "image_url", event.getImageUrl());
        putIfWritable(values, writableColumns, "category_id", event.getCategoryId());
        Integer organizerId = event.getCreatedBy() > 0 ? event.getCreatedBy() : null;
        putIfWritable(values, writableColumns, "created_by", organizerId);
        // Compatibilite avec schemas qui utilisent organizer_id au lieu de created_by.
        putIfWritable(values, writableColumns, "organizer_id", organizerId);
        putIfWritable(values, writableColumns, "organisateur_id", organizerId);
        putIfWritable(values, writableColumns, "status", event.getStatus() != null ? event.getStatus().name() : "DRAFT");
        putIfWritable(values, writableColumns, "is_free", event.isFree());
        putIfWritable(values, writableColumns, "ticket_price", event.getTicketPrice());

        LocalDateTime start = event.getStartDate();
        LocalDateTime end = event.getEndDate();

        putIfWritable(values, writableColumns, "start_date", start != null ? Timestamp.valueOf(start) : null);
        putIfWritable(values, writableColumns, "end_date", end != null ? Timestamp.valueOf(end) : null);
        putIfWritable(values, writableColumns, "start_datetime", start != null ? Timestamp.valueOf(start) : null);
        putIfWritable(values, writableColumns, "end_datetime", end != null ? Timestamp.valueOf(end) : null);
        putIfWritable(values, writableColumns, "date_debut", start != null ? Timestamp.valueOf(start) : null);
        putIfWritable(values, writableColumns, "date_fin", end != null ? Timestamp.valueOf(end) : null);

        putIfWritable(values, writableColumns, "event_date", start != null ? Date.valueOf(start.toLocalDate()) : null);
        putIfWritable(values, writableColumns, "start_time", start != null ? Time.valueOf(start.toLocalTime()) : null);
        putIfWritable(values, writableColumns, "end_time", end != null ? Time.valueOf(end.toLocalTime()) : null);
        putIfWritable(values, writableColumns, "date_event", start != null ? Date.valueOf(start.toLocalDate()) : null);
        putIfWritable(values, writableColumns, "heure_debut", start != null ? Time.valueOf(start.toLocalTime()) : null);
        putIfWritable(values, writableColumns, "heure_fin", end != null ? Time.valueOf(end.toLocalTime()) : null);

        return values;
    }

    private void putIfWritable(Map<String, Object> target, Set<String> writableColumns, String column, Object value) {
        if (writableColumns.contains(column.toLowerCase()) && !target.containsKey(column)) {
            target.put(column, value);
        }
    }

    private void bindValue(PreparedStatement pstmt, int index, Object value) throws SQLException {
        if (value == null) {
            pstmt.setObject(index, null);
            return;
        }
        if (value instanceof String v) {
            pstmt.setString(index, v);
        } else if (value instanceof Integer v) {
            pstmt.setInt(index, v);
        } else if (value instanceof Boolean v) {
            pstmt.setBoolean(index, v);
        } else if (value instanceof Double v) {
            pstmt.setDouble(index, v);
        } else if (value instanceof Timestamp v) {
            pstmt.setTimestamp(index, v);
        } else if (value instanceof Date v) {
            pstmt.setDate(index, v);
        } else if (value instanceof Time v) {
            pstmt.setTime(index, v);
        } else if (value instanceof LocalDateTime v) {
            pstmt.setTimestamp(index, Timestamp.valueOf(v));
        } else if (value instanceof LocalDate v) {
            pstmt.setDate(index, Date.valueOf(v));
        } else if (value instanceof LocalTime v) {
            pstmt.setTime(index, Time.valueOf(v));
        } else {
            pstmt.setObject(index, value);
        }
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
