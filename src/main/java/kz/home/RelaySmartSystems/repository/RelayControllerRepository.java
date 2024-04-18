package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelayControllerRepository extends JpaRepository<RelayController, String> {
    List<RelayController> findByUser(User user);
    RelayController findByMac(String mac);
}
