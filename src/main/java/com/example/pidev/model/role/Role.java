package com.example.pidev.model.role;
import com.example.pidev.model.user.UserModel;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.*;

import java.util.List;


public class Role {
@Id
@GeneratedValue(strategy =GenerationType.IDENTITY)
    private long Id_Role;
    private String RoleName;
    @OneToMany(mappedBy="role")
    private List<UserModel>users;

    public Role() {
        this.RoleName = "DEFAULT"; // ici le rôle par défaut
    }

    public Role(int idRole, String roleName) {
        this.Id_Role = idRole;
        this.RoleName = roleName;
    }
    public Role( String roleName) {

        this.RoleName = roleName;
    }

    public long getId_Role() {
        return Id_Role;
    }

    public void setId_Role(int id_Role) {
        Id_Role = id_Role;
    }

    public String getRoleName() {
        return RoleName;
    }

    public void setRoleName(String roleName) {
        this.RoleName = roleName;
    }
}
