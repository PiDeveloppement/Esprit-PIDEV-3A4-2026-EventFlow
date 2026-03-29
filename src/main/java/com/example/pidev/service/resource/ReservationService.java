package com.example.pidev.service.resource;

import com.example.pidev.model.resource.ReservationResource;
import com.example.pidev.utils.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ReservationService {
    private final Connection connection = DBConnection.getConnection();

    private final String salleNameColumn;
    private final String salleImageColumn;
    private final String equipementNameColumn;
    private final String equipementImageColumn;

    public ReservationService() {
        this.salleNameColumn = resolveColumnName("salle", "name", "nom");
        this.salleImageColumn = resolveColumnName("salle", "image_path", "image", "photo");
        this.equipementNameColumn = resolveColumnName("equipement", "name", "nom");
        this.equipementImageColumn = resolveColumnName("equipement", "image_path", "image", "photo");
    }

    public void ajouter(ReservationResource r) throws SQLException {
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

            if (r.getSalleId() != null) {
                ps.setInt(2, r.getSalleId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            if (r.getEquipementId() != null) {
                ps.setInt(3, r.getEquipementId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setTimestamp(4, Timestamp.valueOf(r.getStartTimedate()));
            ps.setTimestamp(5, Timestamp.valueOf(r.getEndTime()));
            ps.setInt(6, r.getQuantity());

            if (isUpdate) {
                ps.setInt(7, r.getId());
            } else {
                // Keep legacy behavior if no session id is injected at caller side.
                ps.setInt(7, 1);
            }

            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM reservation_resource WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<ReservationResource> afficher() {
        List<ReservationResource> list = new ArrayList<>();

        String salleNameExpr = salleNameColumn != null
                ? "s." + salleNameColumn
                : "CONCAT('Salle #', COALESCE(r.salle_id, 0))";
        String equipNameExpr = equipementNameColumn != null
                ? "e." + equipementNameColumn
                : "CONCAT('Equipement #', COALESCE(r.equipement_id, 0))";
        String salleImageExpr = salleImageColumn != null ? "s." + salleImageColumn : "NULL";
        String equipImageExpr = equipementImageColumn != null ? "e." + equipementImageColumn : "NULL";

        String query = "SELECT r.*, " + salleNameExpr + " AS s_name, " + salleImageExpr + " AS s_img, "
                + equipNameExpr + " AS e_name, " + equipImageExpr + " AS e_img "
                + "FROM reservation_resource r "
                + "LEFT JOIN salle s ON r.salle_id = s.id "
                + "LEFT JOIN equipement e ON r.equipement_id = e.id "
                + "ORDER BY r.id DESC";

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                ReservationResource res = new ReservationResource(
                        rs.getInt("id"),
                        rs.getString("resource_type"),
                        (Integer) rs.getObject("salle_id"),
                        (Integer) rs.getObject("equipement_id"),
                        rs.getTimestamp("reservation_date_start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getInt("quantity")
                );
                res.setResourceName(res.getSalleId() != null ? rs.getString("s_name") : rs.getString("e_name"));
                res.setImagePath(res.getSalleId() != null ? rs.getString("s_img") : rs.getString("e_img"));
                list.add(res);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean isSalleOccupee(int sId, LocalDateTime start, LocalDateTime end, int excludeId) {
        String q = "SELECT COUNT(*) FROM reservation_resource WHERE salle_id = ? AND id != ? AND (reservation_date_start_time < ? AND end_time > ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setInt(1, sId);
            ps.setInt(2, excludeId);
            ps.setTimestamp(3, Timestamp.valueOf(end));
            ps.setTimestamp(4, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public int getStockOccupe(int eqId, LocalDateTime start, LocalDateTime end, int excludeId) {
        String q = "SELECT SUM(quantity) FROM reservation_resource WHERE equipement_id = ? AND id != ? AND (reservation_date_start_time < ? AND end_time > ?)";
        try (PreparedStatement ps = connection.prepareStatement(q)) {
            ps.setInt(1, eqId);
            ps.setInt(2, excludeId);
            ps.setTimestamp(3, Timestamp.valueOf(end));
            ps.setTimestamp(4, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public int getStockTotalEquipement(int eqId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT quantity FROM equipement WHERE id = ?")) {
            ps.setInt(1, eqId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public Map<String, Integer> getStatsByType() {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT resource_type, COUNT(*) AS count FROM reservation_resource GROUP BY resource_type";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString("resource_type"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public Map<String, Integer> getStatsTopResources() {
        Map<String, Integer> stats = new HashMap<>();

        String salleNameExpr = salleNameColumn != null
                ? "s." + salleNameColumn
                : "CONCAT('Salle #', COALESCE(r.salle_id, 0))";
        String equipNameExpr = equipementNameColumn != null
                ? "e." + equipementNameColumn
                : "CONCAT('Equipement #', COALESCE(r.equipement_id, 0))";
        String resourceNameExpr = "COALESCE(" + salleNameExpr + ", " + equipNameExpr + ")";

        String query = "SELECT " + resourceNameExpr + " AS resource_name, COUNT(*) AS count "
                + "FROM reservation_resource r "
                + "LEFT JOIN salle s ON r.salle_id = s.id "
                + "LEFT JOIN equipement e ON r.equipement_id = e.id "
                + "GROUP BY resource_name ORDER BY count DESC LIMIT 5";

        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                stats.put(rs.getString("resource_name"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public int getAvailableResources() {
        String sql = "SELECT (SELECT COUNT(*) FROM equipement WHERE disponible = true) + "
                + "(SELECT COUNT(*) FROM salle WHERE disponible = true) AS total";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAvailableResources: " + e.getMessage());
        }
        return 0;
    }

    public int getAvailableEquipment() {
        String sql = "SELECT COUNT(*) FROM equipement WHERE disponible = true";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAvailableEquipment: " + e.getMessage());
        }
        return 0;
    }

    public int getAvailableRooms() {
        String sql = "SELECT COUNT(*) FROM salle WHERE disponible = true";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getAvailableRooms: " + e.getMessage());
        }
        return 0;
    }

    public int getCurrentReservations() {
        String sql = "SELECT COUNT(*) FROM reservation WHERE NOW() BETWEEN date_debut AND date_fin";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur getCurrentReservations: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalResources() {
        String sql = "SELECT (SELECT COUNT(*) FROM equipement) + (SELECT COUNT(*) FROM salle) AS total";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Erreur getTotalResources: " + e.getMessage());
        }
        return 0;
    }

    private String resolveColumnName(String table, String... candidates) {
        if (connection == null) {
            return null;
        }
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> availableColumns = new HashSet<>();
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, table, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName != null) {
                        availableColumns.add(columnName.toLowerCase(Locale.ROOT));
                    }
                }
            }
            for (String candidate : candidates) {
                if (availableColumns.contains(candidate.toLowerCase(Locale.ROOT))) {
                    return candidate;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture schema table " + table + ": " + e.getMessage());
        }
        return null;
    }
}
