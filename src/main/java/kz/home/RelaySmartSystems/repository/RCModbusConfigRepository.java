package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.relaycontroller.RCModbusConfig;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RCModbusConfigRepository extends JpaRepository<RCModbusConfig, UUID> {
    List<RCModbusConfig> findByMaster(String master);
}
