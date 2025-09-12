package kz.home.RelaySmartSystems.model.entity;

import javax.persistence.*;

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

    public String getFio() {
        return this.firstName + " " + this.lastName;
    }

}
