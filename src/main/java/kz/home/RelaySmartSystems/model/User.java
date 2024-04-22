package kz.home.RelaySmartSystems.model;

import javax.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {
    // @GeneratedValue(strategy = GenerationType.AUTO) // only for long
    @Id
    String id;
//    String username;
    String firstName;
    String lastName;

//    public User(String firstName) {
//        this.firstName = firstName;
//    }
//
//    public User(String firstName, String lastName) {
//        this.firstName = firstName;
//        this.lastName = lastName;
//    }

    public String getId() {
        return id;
    }

    public String getFio() {
        return this.firstName + " " + this.lastName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
