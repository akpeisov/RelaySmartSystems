package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "rc_inputs")
public class RCInput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    private String state;
    private Integer slaveId = 0;
    @OneToMany(mappedBy = "input", cascade = CascadeType.ALL)
    private List<RCEvent> events;
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RelayController relayController;

    public Integer getSlaveId() {
        return slaveId == null ? 0 : slaveId;
    }
}
