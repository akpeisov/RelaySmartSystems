package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.NetworkConfig;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NetworkConfigRepository extends JpaRepository<NetworkConfig, UUID> {
    NetworkConfig findByController(RelayController controller);
}
