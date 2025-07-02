package kz.home.RelaySmartSystems.model;

import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Date;

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

    public WSSession(WebSocketSession session) {
        this.session = session;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
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

    public void setType(String type) {
        this.type = type;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getConnectionDate() {
        return connectionDate;
    }

    public void setConnectionDate(LocalDateTime connectionDate) {
        this.connectionDate = connectionDate;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
