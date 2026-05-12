package com.example.pidev.service.role;

import com.example.pidev.model.role.Role;
import com.example.pidev.model.role.Role;

import com.example.pidev.utils.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class RoleService {
    private final Connection connection;
    public RoleService() throws SQLException {
        this.connection = new DBConnection().getConnection();
    }
    //Add role
    public boolean addRole(Role role) {
        String query = "INSERT INTO role(RoleName) " +
                "VALUES (?)";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, role.getRoleName());


            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 🔹 Supprimer un role
    public boolean deleteRole(long id) {
        String query = "DELETE FROM role WHERE Id_Role=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
//getAll Roles 
public ObservableList<Role> getAllRoles() {
    ObservableList<Role> roles = FXCollections.observableArrayList();
    String query = "SELECT * FROM role";


    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

        while (rs.next()) {
            
            Role role = new Role(
                    rs.getInt("Id_Role"),
                    rs.getString("RoleName")
            );
            

            roles.add(role);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return roles;
}
    // 🔹 Modifier un role
    public boolean updateRole(Role role) {

        String query = "UPDATE role SET RoleName = ? WHERE Id_Role = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, role.getRoleName());
            stmt.setLong(2, role.getId_Role());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }






    public int getRoleIdByName(String roleName) throws SQLException {
        String query = "SELECT id_role FROM role WHERE rolename = ?";

        Connection connection1  =DBConnection.getConnection();
        PreparedStatement ps = connection1.prepareStatement(query);
        ps.setString(1, roleName);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("id_role");
        }
        return -1;
    }
    public ObservableList<String> getAllRoleNames() throws SQLException {

        ObservableList<String> roles = FXCollections.observableArrayList();

        String query = "SELECT rolename FROM role";

        Connection conn = DBConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(query);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            roles.add(rs.getString("rolename"));
        }

        return roles;
    }
    public int getTotalRolesCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM role";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

}
