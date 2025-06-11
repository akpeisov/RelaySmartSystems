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

    @Override
    public boolean equals(Object obj) {
        User user2 = (User)obj;
        return user2.getId().equals(this.getId());
        //return super.equals(obj);
    }

    public String getFio() {
        return this.firstName + " " + this.lastName;
    }

}
