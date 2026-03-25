package com.example.pidev.service.event;

import com.example.pidev.model.event.EventTicket;
import com.example.pidev.utils.DBConnection;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gÃƒÂ©rer les opÃƒÂ©rations CRUD sur les tickets
 * @author Ons Abdesslem
 */
public class EventTicketService {

    private Connection connection;

    // ==================== CONSTRUCTEUR ====================

    public EventTicketService() {
        // Initialiser la connexion
        this.connection = DBConnection.getConnection();
        if (this.connection == null) {
            System.err.println("Ã¢ÂÅ’ Erreur de connexion ÃƒÂ  la base de donnÃƒÂ©es pour EventTicketService");
        } else {
            System.out.println("Ã¢Å“â€¦ Connexion ÃƒÂ©tablie pour EventTicketService");
        }
    }

    // ==================== CREATE ====================

    /**
     * CrÃƒÂ©er un nouveau ticket
     * @param eventId ID de l'ÃƒÂ©vÃƒÂ©nement
     * @param userId ID de l'utilisateur
     * @return Le ticket crÃƒÂ©ÃƒÂ© ou null si erreur
     */
    public EventTicket createTicket(int eventId, int userId) {
        if (connection == null) {
            System.err.println("[Ticket] No database connection");
            return null;
        }

        Integer validEventId = resolveExistingId("event", "id", eventId);
        Integer validUserId = resolveExistingId("user_model", "Id_User", userId);

        if (validEventId == null) {
            System.err.println("[Ticket] No valid event found for ticket creation");
            return null;
        }
        if (validUserId == null) {
            System.err.println("[Ticket] No valid user found for ticket creation");
            return null;
        }
        if (validEventId != eventId) {
            System.err.println("[Ticket] Invalid event_id " + eventId + ", fallback=" + validEventId);
        }
        if (validUserId != userId) {
            System.err.println("[Ticket] Invalid user_id " + userId + ", fallback=" + validUserId);
        }

        String ticketCode = EventTicket.generateTicketCode(validEventId, validUserId);
        String qrUrl = buildQrUrl(ticketCode);

        String sql = "INSERT INTO event_ticket (ticket_code, event_id, user_id, qr_code) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, ticketCode);
            pstmt.setInt(2, validEventId);
            pstmt.setInt(3, validUserId);
            pstmt.setString(4, qrUrl);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        EventTicket ticket = new EventTicket(ticketCode, validEventId, validUserId);
                        ticket.setId(rs.getInt(1));
                        ticket.setCreatedAt(LocalDateTime.now());
                        ticket.setQrCode(qrUrl);

                        System.out.println("[Ticket] Created: " + ticketCode);
                        return ticket;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[Ticket] Creation error: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private Integer resolveExistingId(String table, String idColumn, int preferredId) {
        String existsSql = "SELECT 1 FROM " + table + " WHERE " + idColumn + " = ? LIMIT 1";
        try (PreparedStatement existsStmt = connection.prepareStatement(existsSql)) {
            existsStmt.setInt(1, preferredId);
            try (ResultSet rs = existsStmt.executeQuery()) {
                if (rs.next()) {
                    return preferredId;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Ticket] ID check error in " + table + ": " + e.getMessage());
        }

        String fallbackSql = "SELECT " + idColumn + " FROM " + table + " ORDER BY " + idColumn + " ASC LIMIT 1";
        try (PreparedStatement fallbackStmt = connection.prepareStatement(fallbackSql);
             ResultSet rs = fallbackStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[Ticket] ID fallback error in " + table + ": " + e.getMessage());
        }

        return null;
    }

    private String buildQrUrl(String ticketCode) {
        try {
            // URL qui ouvrira le PDF du billet quand scannÃƒÂ©
            String pdfUrl = "http://localhost:8080/ticket/" +
                    URLEncoder.encode(ticketCode, StandardCharsets.UTF_8) + "/pdf";

            // Encoder l'URL dans le QR
            String encodedUrl = URLEncoder.encode(pdfUrl, StandardCharsets.UTF_8);

            // QuickChart gÃƒÂ©nÃƒÂ¨re le QR contenant l'URL du PDF
            return "https://quickchart.io/qr?text=" + encodedUrl + "&size=200&margin=2";
        } catch (Exception e) {
            System.err.println("Ã¢ÂÅ’ Erreur gÃƒÂ©nÃƒÂ©ration QR URL: " + e.getMessage());
            return null;
        }
    }

    // ==================== READ ====================

    /**
     * RÃƒÂ©cupÃƒÂ©rer tous les tickets
     */
    public List<EventTicket> getAllTickets() {
        List<EventTicket> tickets = new ArrayList<>();

        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return tickets;
        }

        String sql = "SELECT * FROM event_ticket ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tickets.add(extractTicketFromResultSet(rs));
            }

            System.out.println("Ã¢Å“â€¦ " + tickets.size() + " tickets rÃƒÂ©cupÃƒÂ©rÃƒÂ©s");

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur rÃƒÂ©cupÃƒÂ©ration tickets: " + e.getMessage());
            e.printStackTrace();
        }

        return tickets;
    }

    /**
     * RÃƒÂ©cupÃƒÂ©rer un ticket par son ID
     */
    public EventTicket getTicketById(int id) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return null;
        }

        String sql = "SELECT * FROM event_ticket WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractTicketFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur rÃƒÂ©cupÃƒÂ©ration ticket ID=" + id + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * RÃƒÂ©cupÃƒÂ©rer les tickets d'un ÃƒÂ©vÃƒÂ©nement
     */
    public List<EventTicket> getTicketsByEvent(int eventId) {
        List<EventTicket> tickets = new ArrayList<>();

        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return tickets;
        }

        String sql = "SELECT * FROM event_ticket WHERE event_id = ? ORDER BY created_at";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(extractTicketFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur rÃƒÂ©cupÃƒÂ©ration tickets event " + eventId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return tickets;
    }

    /**
     * RÃƒÂ©cupÃƒÂ©rer les tickets d'un utilisateur
     */
    public List<EventTicket> getTicketsByUser(int userId) {
        List<EventTicket> tickets = new ArrayList<>();

        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return tickets;
        }

        String sql = "SELECT * FROM event_ticket WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(extractTicketFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur rÃƒÂ©cupÃƒÂ©ration tickets user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return tickets;
    }

    /**
     * RÃƒÂ©cupÃƒÂ©rer un ticket par son code
     */
    public EventTicket getTicketByCode(String ticketCode) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return null;
        }

        String sql = "SELECT * FROM event_ticket WHERE ticket_code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, ticketCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractTicketFromResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur rÃƒÂ©cupÃƒÂ©ration ticket code " + ticketCode + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ==================== UPDATE ====================

    /**
     * Marquer un ticket comme utilisÃƒÂ© (check-in)
     */
    public boolean markTicketAsUsed(int ticketId) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return false;
        }

        String sql = "UPDATE event_ticket SET is_used = TRUE, used_at = NOW() WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, ticketId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Ã¢Å“â€¦ Ticket ID=" + ticketId + " marquÃƒÂ© comme utilisÃƒÂ©");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur mise ÃƒÂ  jour ticket: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Mettre ÃƒÂ  jour un ticket (admin)
     */
    public boolean updateTicket(EventTicket ticket) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return false;
        }

        String sql = "UPDATE event_ticket SET ticket_code = ?, event_id = ?, user_id = ?, is_used = ?, used_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, ticket.getTicketCode());
            pstmt.setInt(2, ticket.getEventId());
            pstmt.setInt(3, ticket.getUserId());
            pstmt.setBoolean(4, ticket.isUsed());
            pstmt.setTimestamp(5, ticket.getUsedAt() != null ? Timestamp.valueOf(ticket.getUsedAt()) : null);
            pstmt.setInt(6, ticket.getId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Ã¢Å“â€¦ Ticket ID=" + ticket.getId() + " mis ÃƒÂ  jour");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur mise ÃƒÂ  jour ticket: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== DELETE ====================

    /**
     * Supprimer un ticket
     */
    public boolean deleteTicket(int id) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return false;
        }

        String sql = "DELETE FROM event_ticket WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Ã¢Å“â€¦ Ticket supprimÃƒÂ© (ID=" + id + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur suppression ticket: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== STATISTIQUES ====================

    /**
     * Compter le nombre de tickets pour un ÃƒÂ©vÃƒÂ©nement
     */
    public int countTicketsByEvent(int eventId) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM event_ticket WHERE event_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur comptage tickets: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Compter le nombre de tickets utilisÃƒÂ©s (prÃƒÂ©sents) pour un ÃƒÂ©vÃƒÂ©nement
     */
    public int countUsedTicketsByEvent(int eventId) {
        // VÃƒÂ©rifier la connexion
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM event_ticket WHERE event_id = ? AND is_used = TRUE";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, eventId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur comptage tickets utilisÃƒÂ©s: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // ==================== MÃƒâ€°THODES PRIVÃƒâ€°ES ====================

    /**
     * Extraire un ticket depuis un ResultSet
     */
    private EventTicket extractTicketFromResultSet(ResultSet rs) throws SQLException {
        EventTicket ticket = new EventTicket();

        ticket.setId(rs.getInt("id"));
        ticket.setTicketCode(rs.getString("ticket_code"));
        ticket.setEventId(rs.getInt("event_id"));
        ticket.setUserId(rs.getInt("user_id"));
        ticket.setQrCode(rs.getString("qr_code"));
        ticket.setUsed(rs.getBoolean("is_used"));

        Timestamp usedTimestamp = rs.getTimestamp("used_at");
        if (usedTimestamp != null) {
            ticket.setUsedAt(usedTimestamp.toLocalDateTime());
        }

        Timestamp createdTimestamp = rs.getTimestamp("created_at");
        if (createdTimestamp != null) {
            ticket.setCreatedAt(createdTimestamp.toLocalDateTime());
        }

        return ticket;
    }

    // ==================== GÃƒâ€°NÃƒâ€°RATION QR CODES MANQUANTS ====================

    /**
     * GÃƒÂ©nÃƒÂ©rer les QR codes manquants pour les anciens tickets avec qr_code = NULL
     * Utilise QuickChart.io pour gÃƒÂ©nÃƒÂ©rer les QR codes dynamiques
     * @return Le nombre de tickets mis ÃƒÂ  jour
     */
    public int generateMissingQRCodes() {
        if (connection == null) {
            System.err.println("Ã¢ÂÅ’ Pas de connexion ÃƒÂ  la base de donnÃƒÂ©es");
            return 0;
        }

        int updatedCount = 0;

        // 1. RÃƒÂ©cupÃƒÂ©rer tous les tickets sans QR code
        String selectSql = "SELECT id, ticket_code FROM event_ticket WHERE qr_code IS NULL OR qr_code = ''";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {
                int ticketId = rs.getInt("id");
                String ticketCode = rs.getString("ticket_code");

                // 2. GÃƒÂ©nÃƒÂ©rer l'URL du QR code via QuickChart.io
                String qrUrl = buildQrUrl(ticketCode);

                if (qrUrl != null) {
                    // 3. Mettre ÃƒÂ  jour le ticket dans la base de donnÃƒÂ©es
                    String updateSql = "UPDATE event_ticket SET qr_code = ? WHERE id = ?";

                    try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                        pstmt.setString(1, qrUrl);
                        pstmt.setInt(2, ticketId);

                        int rowsAffected = pstmt.executeUpdate();
                        if (rowsAffected > 0) {
                            updatedCount++;
                            System.out.println("Ã¢Å“â€¦ QR code gÃƒÂ©nÃƒÂ©rÃƒÂ© pour ticket ID=" + ticketId + " (" + ticketCode + ")");
                        }
                    }
                } else {
                    System.err.println("Ã¢Å¡Â Ã¯Â¸Â Impossible de gÃƒÂ©nÃƒÂ©rer QR code pour ticket ID=" + ticketId);
                }
            }

            System.out.println("Ã¢Å“â€¦ " + updatedCount + " tickets ont ÃƒÂ©tÃƒÂ© mis ÃƒÂ  jour avec des QR codes");

        } catch (SQLException e) {
            System.err.println("Ã¢ÂÅ’ Erreur gÃƒÂ©nÃƒÂ©ration QR codes manquants: " + e.getMessage());
            e.printStackTrace();
        }

        return updatedCount;
    }
}
