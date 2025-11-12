package kz.home.RelaySmartSystems.model.entity;

import javax.persistence.*;

import kz.home.RelaySmartSystems.model.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID uuid;

    String username;
    String firstName;
    String lastName;
    Role role = Role.ROLE_USER;

    public String getFio() {
        return this.firstName + " " + this.lastName;
    }

    //@Override
    public boolean equals(User u) {
        if (u == null || username == null) {
            return false;
        }
        return username.equalsIgnoreCase(u.getUsername());
    }
}
