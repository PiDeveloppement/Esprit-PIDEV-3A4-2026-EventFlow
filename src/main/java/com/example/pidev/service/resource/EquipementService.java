package com.example.pidev.service.resource;

// CORRECTION : Importation du modèle depuis le bon sous-package
import com.example.pidev.model.resource.Equipement;
import com.example.pidev.utils.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipementService {
    // Utilisation d'une méthode de récupération de connexion plus robuste
    private Connection connection = DBConnection.getConnection();

    public void ajouter(Equipement e) {
        // CORRECTION : Vérifiez que le nom de la colonne est 'equipement_type' ou 'type' dans votre BD
        String sql = "INSERT INTO equipement (name, equipement_type, status, quantity, image_path) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, e.getName());
            pst.setString(2, e.getType());
            pst.setString(3, e.getStatus());
            pst.setInt(4, e.getQuantity());
            pst.setString(5, e.getImagePath());
            pst.executeUpdate();
            System.out.println("Équipement ajouté avec succès !");
        } catch (SQLException ex) {
            System.err.println("Erreur lors de l'ajout : " + ex.getMessage());
        }
    }

    public void modifier(Equipement e) {
        String sql = "UPDATE equipement SET name=?, equipement_type=?, status=?, quantity=?, image_path=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, e.getName());
            pst.setString(2, e.getType());
            pst.setString(3, e.getStatus());
            pst.setInt(4, e.getQuantity());
            pst.setString(5, e.getImagePath());
            pst.setInt(6, e.getId());
            pst.executeUpdate();
            System.out.println("Équipement modifié avec succès !");
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la modification : " + ex.getMessage());
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM equipement WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Équipement supprimé !");
        } catch (SQLException ex) {
            System.err.println("Erreur lors de la suppression : " + ex.getMessage());
        }
    }

    public List<Equipement> afficher() {
        List<Equipement> liste = new ArrayList<>();
        String sql = "SELECT * FROM equipement";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                // Création de l'objet avec le package correct
                liste.add(new Equipement(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("equipement_type"),
                        rs.getString("status"),
                        rs.getInt("quantity"),
                        rs.getString("image_path")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return liste;
    }
}
//prepare statement(taawedh les 4 etapes akther securite) w statement