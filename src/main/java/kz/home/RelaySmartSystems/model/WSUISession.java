package kz.home.RelaySmartSystems.model;

import org.springframework.web.socket.WebSocketSession;

public class WSUISession {
    private WebSocketSession session;
    private String username;
    private User user;

    public WSUISession(WebSocketSession session) {
        this.session = session;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
