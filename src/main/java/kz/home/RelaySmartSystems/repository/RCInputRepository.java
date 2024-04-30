package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.relaycontroller.RCInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface RCInputRepository extends JpaRepository<RCInput, UUID> {
    @Query(value = "select * from rc_inputs o where o.relay_controller_uuid = ?1 and o.id = ?2", nativeQuery = true)
    RCInput findInput(UUID relayControllerId, Integer input);
}
