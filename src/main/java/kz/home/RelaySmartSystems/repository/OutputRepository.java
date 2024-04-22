package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.relaycontroller.Output;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface OutputRepository extends JpaRepository<Output, UUID> {
    //@Query("select o from Output o where o.RelayController = ?1 and o.id = ?2")
    @Query(value = "select * from rc_outputs o where o.relay_controller_uuid = ?1 and o.id = ?2", nativeQuery = true)
    Output findOutput(UUID relayControllerId, Integer output);

    @Query(value = "select * from rc_inputs o where o.relay_controller_uuid = ?1 and o.id = ?2", nativeQuery = true)
    Output findInput(UUID relayControllerId, Integer input);
}
