package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_inputs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCInput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    private String state;
    private Integer slaveId;
    @OneToMany(mappedBy = "input", cascade = CascadeType.ALL, fetch = FetchType.EAGER) // без cascade = CascadeType.ALL при вставке не добавляются rules. Но и не удаляются все... а CascadeType.REMOVE удаляет, но не добавляет, EAGER сразу грузит дочерние объекты из базы, без него получаем ошибку lazyload
    private Set<RCEvent> events;
    @JsonBackReference
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RelayController relayController;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Set<RCEvent> getEvents() {
        return events;
    }

    public void setEvents(Set<RCEvent> events) {
        this.events = events;
    }

    public RelayController getRelayController() {
        return relayController;
    }
    public void setRelayController(RelayController relayController) {
        this.relayController = relayController;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
    public Integer getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

}
