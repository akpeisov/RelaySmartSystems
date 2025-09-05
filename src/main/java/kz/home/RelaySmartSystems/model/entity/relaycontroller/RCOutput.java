package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "rc_outputs")
public class RCOutput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private Long limit;
    private Long timer;
    private String type;
    @JsonProperty("default")
    @Column(name = "default")
    private String _default;
    private String state;
    private Boolean alice;
    private String room;
    private Integer on;
    private Integer off;
    private Integer slaveId = 0;

    @JsonBackReference
    @ManyToOne(optional = false)  //(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    private RelayController relayController;

    public Integer getSlaveId() {
        return slaveId == null ? 0 : slaveId;
    }
}
