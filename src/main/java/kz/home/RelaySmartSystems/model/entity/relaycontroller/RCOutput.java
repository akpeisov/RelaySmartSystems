package kz.home.RelaySmartSystems.model.entity.relaycontroller;

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
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false)
    private String name;
    private Long limit;
    private Long timer;
    @Column(nullable = false)
    private String type = "s";
    @Column(name = "default")
    private String _default;
    private String state;
    private Boolean alice;
    private String room;
    private Integer on;
    private Integer off;
    private Integer slaveId = 0;

    @ManyToOne(optional = false)  //(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    private RelayController relayController;

    public Integer getSlaveId() {
        return slaveId == null ? 0 : slaveId;
    }
}
