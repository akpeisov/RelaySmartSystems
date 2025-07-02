package kz.home.RelaySmartSystems.model.relaycontroller;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "rc_modbus")
public class RCModbus extends RelayController {
    UUID masterUUID;
    Integer slaveId;
}
