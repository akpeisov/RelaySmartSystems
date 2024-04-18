package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.WSSession;
import kz.home.RelaySmartSystems.model.WSTextMessage;
import kz.home.RelaySmartSystems.model.def.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {
    private static final ArrayList<WebSocketSession> sessions = new ArrayList<WebSocketSession>();
    private static final ArrayList<WSSession> wsSessions = new ArrayList<WSSession>();
    private static final Logger logger = LoggerFactory.getLogger(SocketHandler.class);
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.info(payload);

        // TODO : add catch for incorrect data
        ObjectMapper objectMapper = new ObjectMapper();
        WSTextMessage wsTextMessage = objectMapper.readValue(payload, WSTextMessage.class);
        byte[] json = objectMapper.writeValueAsBytes(wsTextMessage.getPayload());

        if ("INFO".equals(wsTextMessage.getType())) {
            logger.info(wsTextMessage.getPayload().toString());
            //Info info = (Info)wsTextMessage.getPayload();
            Info info = objectMapper.readValue(json, Info.class);
            setControllerIdForSession(session, info.getId());
        }

        //session.sendMessage(new TextMessage("Received: " + payload));
        //logger.info(session.getId());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        wsSessions.add(new WSSession(session));
        logger.info(String.format("New client connected with ID %s", session.getId()));
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        wsSessions.remove(new WSSession(session));
        logger.info(String.format("Client disconnected with ID %s", session.getId()));
        super.afterConnectionClosed(session, status);
    }

    public String sendMessageToUser(String sessionId, String message) {
        for (WebSocketSession session: sessions) {
            if (session.getId().equals(sessionId)) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        return "OK";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return "ERROR";
                }
                break;
            }
        }
        return "NOT_FOUND";
    }

    public String sendMessageToUser2(String controllerId, String message) {
        if (controllerId == null)
            return "ERROR";

        for (WSSession session: wsSessions) {
            if (controllerId.equals(session.getControllerId())) {
                try {
                    if (session.getSession().isOpen()) {
                        session.getSession().sendMessage(new TextMessage(message));
                        return "OK";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return "ERROR";
                }
                break;
            }
        }
        return "NOT_FOUND";
    }

    private void setControllerIdForSession(WebSocketSession targetSession, String controllerId) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setControllerId(controllerId);
                break; // если нужно устанавливать только один раз
            }
        }
    }

}
