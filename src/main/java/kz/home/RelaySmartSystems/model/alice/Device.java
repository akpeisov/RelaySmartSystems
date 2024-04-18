package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import kz.home.RelaySmartSystems.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "alice_devices")
public class Device {
    @Id
    private String id;
    private String name;
    private String description;
    private String type;

    @ManyToOne
    @JsonIgnore
    private User user;
    private String room;

    // mappedby чтобы прилетал айдишник в умения при создании
    // CascadeType.ALL
    // FetchType.EAGER чтобы при выборке у устройства подтягивались умения сразу
    @JsonManagedReference
    @OneToMany(mappedBy = "device",cascade = CascadeType.ALL, fetch = FetchType.EAGER) //, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Capability> capabilities;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public List<Capability> getCapabilities() {
        return capabilities;
    }
    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    public String getDescription() {
        //return description;
        return description == null ? name : description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

}
