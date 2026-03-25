package com.example.pidev.service.resource;

import com.example.pidev.model.resource.Salle;
import com.example.pidev.utils.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalleService {
    private final Connection connection = DBConnection.getConnection();
    private volatile ColumnMapping columnMapping;

    public void ajouter(Salle s) {
        try {
            ColumnMapping mapping = getColumnMapping();
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            addValue(columns, values, mapping.nameCol, s.getName());
            addValue(columns, values, mapping.capacityCol, s.getCapacity());
            addValue(columns, values, mapping.buildingCol, s.getBuilding());
            addValue(columns, values, mapping.floorCol, s.getFloor());
            if (mapping.statusCol != null) {
                Object statusValue = mapping.statusIsBoolean
                        ? isAvailableStatus(s.getStatus())
                        : s.getStatus();
                addValue(columns, values, mapping.statusCol, statusValue);
            }
            addValue(columns, values, mapping.imagePathCol, s.getImagePath());
            addValue(columns, values, mapping.latitudeCol, s.getLatitude());
            addValue(columns, values, mapping.longitudeCol, s.getLongitude());

            if (columns.isEmpty()) {
                throw new SQLException("Aucune colonne exploitable detectee pour INSERT sur salle.");
            }

            String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
            String sql = "INSERT INTO salle (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";

            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                bindValues(pst, values);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modifier(Salle s) {
        try {
            ColumnMapping mapping = getColumnMapping();
            List<String> assignments = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            addAssignment(assignments, values, mapping.nameCol, s.getName());
            addAssignment(assignments, values, mapping.capacityCol, s.getCapacity());
            addAssignment(assignments, values, mapping.buildingCol, s.getBuilding());
            addAssignment(assignments, values, mapping.floorCol, s.getFloor());
            if (mapping.statusCol != null) {
                Object statusValue = mapping.statusIsBoolean
                        ? isAvailableStatus(s.getStatus())
                        : s.getStatus();
                addAssignment(assignments, values, mapping.statusCol, statusValue);
            }
            addAssignment(assignments, values, mapping.imagePathCol, s.getImagePath());
            addAssignment(assignments, values, mapping.latitudeCol, s.getLatitude());
            addAssignment(assignments, values, mapping.longitudeCol, s.getLongitude());

            if (assignments.isEmpty()) {
                throw new SQLException("Aucune colonne exploitable detectee pour UPDATE sur salle.");
            }

            String sql = "UPDATE salle SET " + String.join(", ", assignments) + " WHERE " + mapping.idCol + " = ?";
            values.add(s.getId());

            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                bindValues(pst, values);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Salle> afficher() {
        List<Salle> liste = new ArrayList<>();
        String sql = "SELECT * FROM salle";

        try {
            ColumnMapping mapping = getColumnMapping();
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    int id = getIntValue(rs, mapping.idCol, 0);
                    String name = getStringValue(rs, mapping.nameCol, "");
                    int capacity = getIntValue(rs, mapping.capacityCol, 0);
                    String building = getStringValue(rs, mapping.buildingCol, "");
                    int floor = getIntValue(rs, mapping.floorCol, 0);
                    String status = getStatusValue(rs, mapping);
                    String imagePath = getStringValue(rs, mapping.imagePathCol, "");
                    double latitude = getDoubleValue(rs, mapping.latitudeCol, 0.0);
                    double longitude = getDoubleValue(rs, mapping.longitudeCol, 0.0);

                    liste.add(new Salle(
                            id, name, capacity, building, floor, status, imagePath, latitude, longitude
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

    public void supprimer(int id) {
        try {
            ColumnMapping mapping = getColumnMapping();
            String sql = "DELETE FROM salle WHERE " + mapping.idCol + " = ?";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setInt(1, id);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private ColumnMapping getColumnMapping() throws SQLException {
        if (columnMapping != null) {
            return columnMapping;
        }
        synchronized (this) {
            if (columnMapping == null) {
                columnMapping = loadColumnMapping();
            }
            return columnMapping;
        }
    }

    private ColumnMapping loadColumnMapping() throws SQLException {
        if (connection == null) {
            throw new SQLException("Connexion DB indisponible.");
        }

        Map<String, ColumnInfo> columns = new HashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();

        try (ResultSet rs = metaData.getColumns(catalog, null, "salle", null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int columnType = rs.getInt("DATA_TYPE");
                columns.put(columnName.toLowerCase(Locale.ROOT), new ColumnInfo(columnName, columnType));
            }
        }

        if (columns.isEmpty()) {
            try (ResultSet rs = metaData.getColumns(catalog, null, "SALLE", null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    int columnType = rs.getInt("DATA_TYPE");
                    columns.put(columnName.toLowerCase(Locale.ROOT), new ColumnInfo(columnName, columnType));
                }
            }
        }

        if (columns.isEmpty()) {
            throw new SQLException("Table 'salle' introuvable.");
        }

        String idCol = pickColumn(columns, "id", "id_salle");
        if (idCol == null) {
            throw new SQLException("Colonne ID introuvable pour table salle.");
        }

        String nameCol = pickColumn(columns, "name", "nom", "nom_salle");
        String capacityCol = pickColumn(columns, "capacity", "capacite", "nbr_places");
        String buildingCol = pickColumn(columns, "building", "batiment", "bloc");
        String floorCol = pickColumn(columns, "floor", "etage");
        String statusCol = pickColumn(columns, "status", "statut", "disponible");
        String imagePathCol = pickColumn(columns, "image_path", "image", "image_url", "photo");
        String latitudeCol = pickColumn(columns, "latitude", "lat");
        String longitudeCol = pickColumn(columns, "longitude", "lon", "lng");

        boolean statusIsBoolean = false;
        if (statusCol != null) {
            ColumnInfo info = columns.get(statusCol.toLowerCase(Locale.ROOT));
            statusIsBoolean = info != null && (
                    info.sqlType == Types.BOOLEAN ||
                    info.sqlType == Types.BIT ||
                    info.sqlType == Types.TINYINT
            );
        }

        return new ColumnMapping(
                idCol, nameCol, capacityCol, buildingCol, floorCol,
                statusCol, statusIsBoolean, imagePathCol, latitudeCol, longitudeCol
        );
    }

    private String pickColumn(Map<String, ColumnInfo> columns, String... candidates) {
        for (String candidate : candidates) {
            ColumnInfo info = columns.get(candidate.toLowerCase(Locale.ROOT));
            if (info != null) {
                return info.name;
            }
        }
        return null;
    }

    private void addValue(List<String> columns, List<Object> values, String columnName, Object value) {
        if (columnName == null) {
            return;
        }
        columns.add(columnName);
        values.add(value);
    }

    private void addAssignment(List<String> assignments, List<Object> values, String columnName, Object value) {
        if (columnName == null) {
            return;
        }
        assignments.add(columnName + " = ?");
        values.add(value);
    }

    private void bindValues(PreparedStatement pst, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            int index = i + 1;
            if (value == null) {
                pst.setObject(index, null);
            } else if (value instanceof Integer v) {
                pst.setInt(index, v);
            } else if (value instanceof Double v) {
                pst.setDouble(index, v);
            } else if (value instanceof Boolean v) {
                pst.setBoolean(index, v);
            } else if (value instanceof String v) {
                pst.setString(index, v);
            } else {
                pst.setObject(index, value);
            }
        }
    }

    private boolean isAvailableStatus(String status) {
        if (status == null) {
            return true;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("DISPONIBLE")
                || normalized.equals("LIBRE")
                || normalized.equals("AVAILABLE")
                || normalized.equals("ACTIVE")
                || normalized.equals("1")
                || normalized.equals("TRUE");
    }

    private String getStatusValue(ResultSet rs, ColumnMapping mapping) throws SQLException {
        if (mapping.statusCol == null) {
            return "DISPONIBLE";
        }
        if (mapping.statusIsBoolean) {
            boolean available = rs.getBoolean(mapping.statusCol);
            if (rs.wasNull()) {
                return "DISPONIBLE";
            }
            return available ? "DISPONIBLE" : "OCCUPEE";
        }
        String value = rs.getString(mapping.statusCol);
        return value != null ? value : "DISPONIBLE";
    }

    private String getStringValue(ResultSet rs, String columnName, String defaultValue) throws SQLException {
        if (columnName == null) {
            return defaultValue;
        }
        String value = rs.getString(columnName);
        return value != null ? value : defaultValue;
    }

    private int getIntValue(ResultSet rs, String columnName, int defaultValue) throws SQLException {
        if (columnName == null) {
            return defaultValue;
        }
        int value = rs.getInt(columnName);
        return rs.wasNull() ? defaultValue : value;
    }

    private double getDoubleValue(ResultSet rs, String columnName, double defaultValue) throws SQLException {
        if (columnName == null) {
            return defaultValue;
        }
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? defaultValue : value;
    }

    private static class ColumnInfo {
        private final String name;
        private final int sqlType;

        private ColumnInfo(String name, int sqlType) {
            this.name = name;
            this.sqlType = sqlType;
        }
    }

    private static class ColumnMapping {
        private final String idCol;
        private final String nameCol;
        private final String capacityCol;
        private final String buildingCol;
        private final String floorCol;
        private final String statusCol;
        private final boolean statusIsBoolean;
        private final String imagePathCol;
        private final String latitudeCol;
        private final String longitudeCol;

        private ColumnMapping(
                String idCol,
                String nameCol,
                String capacityCol,
                String buildingCol,
                String floorCol,
                String statusCol,
                boolean statusIsBoolean,
                String imagePathCol,
                String latitudeCol,
                String longitudeCol
        ) {
            this.idCol = idCol;
            this.nameCol = nameCol;
            this.capacityCol = capacityCol;
            this.buildingCol = buildingCol;
            this.floorCol = floorCol;
            this.statusCol = statusCol;
            this.statusIsBoolean = statusIsBoolean;
            this.imagePathCol = imagePathCol;
            this.latitudeCol = latitudeCol;
            this.longitudeCol = longitudeCol;
        }
    }
}
