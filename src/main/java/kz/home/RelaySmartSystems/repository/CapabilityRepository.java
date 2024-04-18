package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.alice.Capability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface CapabilityRepository extends JpaRepository<Capability, Long> {
    Capability save(Capability capability);
    @Query("delete from Capability c where c.id = ?1")
    void deleteCapability(Long id);
}
