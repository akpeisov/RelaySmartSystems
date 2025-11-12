package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.lang.annotation.Native;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    @Query("SELECT s FROM Session s WHERE s.sessionId = ?1")
    Session findSessionIdBySessionId(String sessionId);

    @Transactional
    @Query("UPDATE Session s SET s.endDate = CURRENT_TIMESTAMP, s.status = 'TERMINATED' WHERE s.endDate IS NULL")
    void endAllSessions();
}
