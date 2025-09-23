package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    //public Session findBySessionId(String sessionId);
}
