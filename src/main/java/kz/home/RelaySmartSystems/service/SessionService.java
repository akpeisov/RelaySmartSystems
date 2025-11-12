package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.entity.Session;
import kz.home.RelaySmartSystems.repository.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void addSession(String sessionId, String remoteAddr) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setRemoteAddress(remoteAddr);
        session.setStartDate(new java.util.Date());
        session.setStatus("ACTIVE");
        sessionRepository.save(session);
    }

    public void endSession(String sessionId) {
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
//        Session session = sessionRepository.findAll().stream()
//                .filter(s -> s.getSessionId().equals(sessionId))
//                .findFirst()
//                .orElse(null);
        if (session != null) {
            session.setEndDate(new java.util.Date());
            session.setStatus("FINISHED");
            sessionRepository.save(session);
        }
    }

    public void endAllSessions() {
        sessionRepository.endAllSessions();
//        for (Session session : sessionRepository.findAll()) {
//            if (session.getEndDate() == null) {
//                session.setEndDate(new java.util.Date());
//                sessionRepository.save(session);
//            }
//        }
    }

    public void updateLastActive(String sessionId) {
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
//        Session session = sessionRepository.findAll().stream()
//                .filter(s -> s.getSessionId().equals(sessionId))
//                .findFirst()
//                .orElse(null);
        if (session != null) {
            session.setLastActiveDate(new java.util.Date());
            sessionRepository.save(session);
        }
    }

    public void setUsername(String sessionId, String username) {
//        Session session = sessionRepository.findAll().stream()
//                .filter(s -> s.getSessionId().equals(sessionId))
//                .findFirst()
//                .orElse(null);
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        if (session != null) {
            session.setUsername(username);
            session.setType("web");
            sessionRepository.save(session);
        }
    }

    public void setMac(String sessionId, String mac) {
//        Session session = sessionRepository.findAll().stream()
//                .filter(s -> s.getSessionId().equals(sessionId))
//                .findFirst()
//                .orElse(null);
        Session session = sessionRepository.findSessionIdBySessionId(sessionId);
        if (session != null) {
            session.setMac(mac);
            session.setType("controller");
            sessionRepository.save(session);
        }
    }
}
