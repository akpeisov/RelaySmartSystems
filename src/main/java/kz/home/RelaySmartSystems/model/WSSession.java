package kz.home.RelaySmartSystems.model;

import kz.home.RelaySmartSystems.model.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

@Setter
@Getter
public class WSSession {
    private WebSocketSession session;
    private String controllerId;
    private String type;
    private User user;
    private String username;
    private LocalDateTime connectionDate;
    private String clientIP;
    private boolean authorized = false;
    private Object obj;
    private Date lastSend = new Date();

    public WSSession(WebSocketSession session) {
        this.session = session;
        this.connectionDate = LocalDateTime.now();
    }

    public String getControllerId() {
        return controllerId == null ? null : controllerId.toUpperCase();
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId.toUpperCase();
    }

    public String getType() {
        return type == null ? null : type.toLowerCase();
    }

    public boolean isExpired() {
        Date now = new Date();
        return now.getTime() - lastSend.getTime() > 55 * 1000;
    }

    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        lastSend = new Date();
        session.sendMessage(message);
    }
}
