package kz.home.RelaySmartSystems.model;

import org.springframework.web.socket.WebSocketSession;

public class WSSession {
    private WebSocketSession session;
    private String controllerId;

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
        return controllerId;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }
}
