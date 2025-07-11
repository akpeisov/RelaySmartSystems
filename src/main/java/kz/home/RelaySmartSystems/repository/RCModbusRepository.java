package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.relaycontroller.RCModbus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RCModbusRepository extends JpaRepository<RCModbus, UUID> {
    RCModbus findBySlaveUUID(UUID slaveUUID);
}
