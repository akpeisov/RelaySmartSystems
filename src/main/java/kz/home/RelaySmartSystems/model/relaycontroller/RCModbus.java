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
    @JoinColumn(name = "master", unique = true)
    RelayController master;

    @OneToOne
    @JoinColumn(name = "slave", unique = true)
    private RelayController slave;

    Integer slaveId;
}
