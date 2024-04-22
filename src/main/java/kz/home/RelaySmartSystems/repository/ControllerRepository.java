package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.Controller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ControllerRepository extends JpaRepository<Controller, UUID> {
    Controller findByMac(String mac);
}

