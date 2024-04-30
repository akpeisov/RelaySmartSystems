package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.relaycontroller.RCInput;
import kz.home.RelaySmartSystems.model.relaycontroller.RCRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface RCRuleRepository extends JpaRepository<RCRule, UUID> {
    @Query(value = "delete from rc_rules where uuid = ?1", nativeQuery = true)
    @Modifying
    @Transactional
    void deleteRule(UUID id);
}
