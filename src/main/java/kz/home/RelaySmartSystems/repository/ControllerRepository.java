package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.Controller;
import kz.home.RelaySmartSystems.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControllerRepository extends JpaRepository<Controller, UUID> {
    Controller findByMac(String mac);
    List<Controller> findByUser(User user);
}

