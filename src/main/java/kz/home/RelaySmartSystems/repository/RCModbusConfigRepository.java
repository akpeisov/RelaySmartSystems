package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCModbusConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RCModbusConfigRepository extends JpaRepository<RCModbusConfig, UUID> {
    List<RCModbusConfig> findByMaster(String master);
}
