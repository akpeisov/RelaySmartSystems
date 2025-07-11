package kz.home.RelaySmartSystems.model.relaycontroller;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "rc_modbus")
public class RCModbus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID uuid;

    @OneToOne
    RelayController master;
    //UUID masterUUID;

//    @OneToMany
//    RelayController slave;

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<RelayController> slaves;

    //UUID slaveUUID;
    Integer slaveId;
}
