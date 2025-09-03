package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.relaycontroller.RCScheduler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RCSchedulerRepository extends JpaRepository<RCScheduler, UUID> {
}
