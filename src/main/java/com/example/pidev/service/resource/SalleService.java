package com.example.pidev.service.resource;

import com.example.pidev.model.resource.Salle;
import com.example.pidev.utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalleService {
    private Connection connection = DBConnection.getConnection();

    public void ajouter(Salle s) {
        String sql = "INSERT INTO salle (name, capacity, building, floor, status, image_path, latitude, longitude) VALUES (?,?,?,?,?,?,?,?)";
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
        String sql = "UPDATE salle SET name=?, capacity=?, building=?, floor=?, status=?, image_path=?, latitude=?, longitude=? WHERE id=?";
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
        String sql = "SELECT * FROM salle";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(new Salle(
                        rs.getInt("id"), rs.getString("name"), rs.getInt("capacity"),
                        rs.getString("building"), rs.getInt("floor"),
                        rs.getString("status"), rs.getString("image_path"),
                        rs.getDouble("latitude"), rs.getDouble("longitude")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return liste;
    }

    public void supprimer(int id) {
        try (PreparedStatement pst = connection.prepareStatement("DELETE FROM salle WHERE id=?")) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}