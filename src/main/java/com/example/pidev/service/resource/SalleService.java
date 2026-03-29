package com.example.pidev.service.resource;

import com.example.pidev.model.resource.Salle;
import com.example.pidev.utils.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SalleService {
    private final Connection connection = DBConnection.getConnection();
    private final String tableName;

    private final String nameColumn;
    private final String capacityColumn;
    private final String buildingColumn;
    private final String floorColumn;
    private final String statusColumn;
    private final String imageColumn;
    private final String latitudeColumn;
    private final String longitudeColumn;

    public SalleService() {
        this.tableName = resolveTableName("salle", "salles");
        this.nameColumn = resolveColumnName(tableName, "nom", "name");
        this.capacityColumn = resolveColumnName(tableName, "capacite", "capacity");
        this.buildingColumn = resolveColumnName(tableName, "batiment", "building", "bloc");
        this.floorColumn = resolveColumnName(tableName, "etage", "floor");
        this.statusColumn = resolveColumnName(tableName, "statut", "status");
        this.imageColumn = resolveColumnName(tableName, "image_path", "image", "photo");
        this.latitudeColumn = resolveColumnName(tableName, "latitude", "lat");
        this.longitudeColumn = resolveColumnName(tableName, "longitude", "lng");
    }

    public void ajouter(Salle s) {
        String sql = "INSERT INTO " + tableName + " (" + col(nameColumn, "name") + ", " + col(capacityColumn, "capacity") + ", "
                + col(buildingColumn, "building") + ", " + col(floorColumn, "floor") + ", "
                + col(statusColumn, "status") + ", " + col(imageColumn, "image_path") + ", "
                + col(latitudeColumn, "latitude") + ", " + col(longitudeColumn, "longitude") + ") VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, s.getName());
            pst.setInt(2, s.getCapacity());
            pst.setString(3, s.getBuilding());
            pst.setInt(4, s.getFloor());
            pst.setString(5, s.getStatus());
            pst.setString(6, s.getImagePath());
            pst.setDouble(7, s.getLatitude());
            pst.setDouble(8, s.getLongitude());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifier(Salle s) {
        String sql = "UPDATE " + tableName + " SET "
                + col(nameColumn, "name") + "=?, "
                + col(capacityColumn, "capacity") + "=?, "
                + col(buildingColumn, "building") + "=?, "
                + col(floorColumn, "floor") + "=?, "
                + col(statusColumn, "status") + "=?, "
                + col(imageColumn, "image_path") + "=?, "
                + col(latitudeColumn, "latitude") + "=?, "
                + col(longitudeColumn, "longitude") + "=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, s.getName());
            pst.setInt(2, s.getCapacity());
            pst.setString(3, s.getBuilding());
            pst.setInt(4, s.getFloor());
            pst.setString(5, s.getStatus());
            pst.setString(6, s.getImagePath());
            pst.setDouble(7, s.getLatitude());
            pst.setDouble(8, s.getLongitude());
            pst.setInt(9, s.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Salle> afficher() {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT id, "
                + col(nameColumn, "name") + " AS room_name, "
                + col(capacityColumn, "capacity") + " AS room_capacity, "
                + col(buildingColumn, "building") + " AS room_building, "
                + col(floorColumn, "floor") + " AS room_floor, "
                + col(statusColumn, "status") + " AS room_status, "
                + col(imageColumn, "image_path") + " AS room_image, "
                + col(latitudeColumn, "latitude") + " AS room_latitude, "
                + col(longitudeColumn, "longitude") + " AS room_longitude "
                + "FROM " + tableName;
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(new Salle(
                        rs.getInt("id"),
                        rs.getString("room_name"),
                        rs.getInt("room_capacity"),
                        rs.getString("room_building"),
                        rs.getInt("room_floor"),
                        rs.getString("room_status"),
                        rs.getString("room_image"),
                        rs.getDouble("room_latitude"),
                        rs.getDouble("room_longitude")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

    public void supprimer(int id) {
        try (PreparedStatement pst = connection.prepareStatement("DELETE FROM " + tableName + " WHERE id=?")) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String col(String resolved, String fallback) {
        return resolved != null ? resolved : fallback;
    }

    private String resolveColumnName(String table, String... candidates) {
        if (connection == null || candidates == null || candidates.length == 0) {
            return null;
        }
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> availableColumns = new HashSet<>();

            String[] tablePatterns = {table, table.toLowerCase(Locale.ROOT), table.toUpperCase(Locale.ROOT)};
            for (String pattern : tablePatterns) {
                try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, pattern, null)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        if (columnName != null) {
                            availableColumns.add(columnName.toLowerCase(Locale.ROOT));
                        }
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

        // Fallback robuste: sonde SQL directe pour supporter des schemas non exposes via metadata.
        for (String candidate : candidates) {
            if (columnExists(table, candidate)) {
                return candidate;
            }
        }

        // Dernier fallback: utiliser le premier candidat pour garder le comportement historique.
        return candidates[0];
    }

    private String resolveTableName(String... candidates) {
        if (connection == null || candidates == null || candidates.length == 0) {
            return "salle";
        }

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> availableTables = new HashSet<>();
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String table = rs.getString("TABLE_NAME");
                    if (table != null) {
                        availableTables.add(table.toLowerCase(Locale.ROOT));
                    }
                }
            }

            for (String candidate : candidates) {
                if (availableTables.contains(candidate.toLowerCase(Locale.ROOT))) {
                    return candidate;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture schema tables: " + e.getMessage());
        }

        for (String candidate : candidates) {
            if (tableExists(candidate)) {
                return candidate;
            }
        }

        return candidates[0];
    }

    private boolean tableExists(String table) {
        if (connection == null) {
            return false;
        }
        String probe = "SELECT 1 FROM `" + table + "` LIMIT 1";
        try (Statement st = connection.createStatement()) {
            st.executeQuery(probe);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean columnExists(String table, String column) {
        if (connection == null) {
            return false;
        }
        String probe = "SELECT `" + column + "` FROM `" + table + "` LIMIT 1";
        try (Statement st = connection.createStatement()) {
            st.executeQuery(probe);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }
}
