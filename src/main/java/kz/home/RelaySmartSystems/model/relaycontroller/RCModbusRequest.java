package kz.home.RelaySmartSystems.model.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RCModbusRequest {
    UUID masterUUID;
    UUID slaveUUID;
    Integer slaveId;
}
