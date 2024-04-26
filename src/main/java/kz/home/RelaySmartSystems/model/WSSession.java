package kz.home.RelaySmartSystems.model;

import org.springframework.web.socket.WebSocketSession;

public class WSSession {
    private WebSocketSession session;
    private String controllerId;
    private String type;
    private User user;
    private String username;

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
        return type;
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
}
