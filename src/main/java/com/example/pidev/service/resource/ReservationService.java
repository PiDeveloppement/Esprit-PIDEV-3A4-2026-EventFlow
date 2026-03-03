package com.example.pidev.service.resource;

import com.example.pidev.model.resource.ReservationResource;
import com.example.pidev.utils.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ReservationService {
    private Connection connection = DBConnection.getConnection();

    public void ajouter(ReservationResource r) throws SQLException {
        // Ajout de user_id dans la requête
        String query = "INSERT INTO reservation_resource (resource_type, salle_id, equipement_id, reservation_date_start_time, end_time, quantity, user_id) VALUES (?,?,?,?,?,?,?)";
        save(r, query, false);
    }

    public void modifier(ReservationResource r) throws SQLException {
        String query = "UPDATE reservation_resource SET resource_type=?, salle_id=?, equipement_id=?, reservation_date_start_time=?, end_time=?, quantity=? WHERE id=?";
        save(r, query, true);
    }

    private void save(ReservationResource r, String query, boolean isUpdate) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, r.getResourceType());

            if (r.getSalleId() != null) ps.setInt(2, r.getSalleId());
            else ps.setNull(2, Types.INTEGER);

            if (r.getEquipementId() != null) ps.setInt(3, r.getEquipementId());
            else ps.setNull(3, Types.INTEGER);

            ps.setTimestamp(4, Timestamp.valueOf(r.getStartTimedate()));
            ps.setTimestamp(5, Timestamp.valueOf(r.getEndTime()));
            ps.setInt(6, r.getQuantity());

            if (isUpdate) {
                ps.setInt(7, r.getId());
            } else {
                // IMPORTANT: On envoie l'ID de l'utilisateur (1 par défaut ici)
                // Si tu as un système de session, remplace 1 par l'ID de l'utilisateur connecté
                ps.setInt(7, 1);
            }

            ps.executeUpdate();
        }
    }

    // ... (Le reste de tes méthodes supprimer, afficher, etc. restent inchangées)

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM reservation_resource WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<ReservationResource> afficher() {
        List<ReservationResource> list = new ArrayList<>();
        String query = "SELECT r.*, s.name as s_name, s.image_path as s_img, e.name as e_name, e.image_path as e_img " +
                "FROM reservation_resource r LEFT JOIN salle s ON r.salle_id = s.id LEFT JOIN equipement e ON r.equipement_id = e.id ORDER BY r.id DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                ReservationResource res = new ReservationResource(
                        rs.getInt("id"), rs.getString("resource_type"),
                        (Integer) rs.getObject("salle_id"), (Integer) rs.getObject("equipement_id"),
                        rs.getTimestamp("reservation_date_start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(), rs.getInt("quantity")
                );
                res.setResourceName(res.getSalleId() != null ? rs.getString("s_name") : rs.getString("e_name"));
                res.setImagePath(res.getSalleId() != null ? rs.getString("s_img") : rs.getString("e_img"));
                list.add(res);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean isSalleOccupee(int sId, LocalDateTime start, LocalDateTime end, int excludeId) {
        String q = "SELECT COUNT(*) FROM reservation_resource WHERE salle_id = ? AND id != ? AND (reservation_date_start_time < ? AND end_time > ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setInt(1, sId); ps.setInt(2, excludeId);
            ps.setTimestamp(3, Timestamp.valueOf(end)); ps.setTimestamp(4, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    public int getStockOccupe(int eqId, LocalDateTime start, LocalDateTime end, int excludeId) {
        String q = "SELECT SUM(quantity) FROM reservation_resource WHERE equipement_id = ? AND id != ? AND (reservation_date_start_time < ? AND end_time > ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setInt(1, eqId); ps.setInt(2, excludeId);
            ps.setTimestamp(3, Timestamp.valueOf(end)); ps.setTimestamp(4, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }

    public int getStockTotalEquipement(int eqId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT quantity FROM equipement WHERE id = ?")) {
            ps.setInt(1, eqId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { return 0; }
    }
    public Map<String, Integer> getStatsByType() {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT resource_type, COUNT(*) as count FROM reservation_resource GROUP BY resource_type";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString("resource_type"), rs.getInt("count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public Map<String, Integer> getStatsTopResources() {
        Map<String, Integer> stats = new HashMap<>();
        // Cette requête récupère le nom des ressources les plus réservées
        String query = "SELECT COALESCE(s.name, e.name) as name, COUNT(*) as count " +
                "FROM reservation_resource r " +
                "LEFT JOIN salle s ON r.salle_id = s.id " +
                "LEFT JOIN equipement e ON r.equipement_id = e.id " +
                "GROUP BY name ORDER BY count DESC LIMIT 5";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString("name"), rs.getInt("count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }
}