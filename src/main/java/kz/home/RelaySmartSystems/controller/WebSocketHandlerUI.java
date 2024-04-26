package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.filters.JwtAuthorizationFilter;
import kz.home.RelaySmartSystems.model.*;
import kz.home.RelaySmartSystems.model.def.Action;
import kz.home.RelaySmartSystems.model.def.Hello;
import kz.home.RelaySmartSystems.model.relaycontroller.RCUpdateMessage;
import kz.home.RelaySmartSystems.service.ControllerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.random.RandomGenerator;

// класс для веб-интерфейса
@Component
public class WebSocketHandlerUI extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandlerUI.class);
    private static final ArrayList<WSUISession> wsSessions = new ArrayList<WSUISession>();
    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final ControllerService сontrollerService;
    private final ApplicationEventPublisher eventPublisher;

    public WebSocketHandlerUI(JwtAuthorizationFilter jwtAuthorizationFilter,
                              ControllerService сontrollerService, ApplicationEventPublisher eventPublisher) {
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
        this.сontrollerService = сontrollerService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // find current wsSession for session
        //logger.info(String.format("UI msg %s", message.getPayload()));
        //session.sendMessage(new TextMessage("London is the capital of GB"));

        // find current wsSession for session
        WSUISession wsSession = null;
        for (WSUISession wsSession1 : wsSessions) {
            if (wsSession1.getSession().equals(session)) {
                wsSession = wsSession1;
                break;
            }
        }
        if (wsSession == null) {
            logger.error("wsSession not found!");
            session.sendMessage(new TextMessage(ErrorMessage.makeError("WS session not found!")));
            session.close();
            return;
        }
        // authorization part
        // TODO : implement this part
        //wsSession.setUsername("user");

        String payload = message.getPayload();
        byte[] json = null;
        WSTextMessage wsTextMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wsTextMessage = objectMapper.readValue(payload, WSTextMessage.class);
            json = objectMapper.writeValueAsBytes(wsTextMessage.getPayload());
        } catch (Exception e) {
            session.sendMessage(new TextMessage(ErrorMessage.makeError("Wrong message type!")));
            session.close();
            return;
        }

        String type = wsTextMessage.getType();
        if (type == null) {
            return;
        }
        if (!"HELLO".equals(type) && wsSession.getUsername() == null) {
            session.sendMessage(new TextMessage(ErrorMessage.makeError("Session is not authorized!")));
            session.close();
            return;
        }

        switch (type) {
            case "HELLO":
                //logger.info(wsTextMessage.getPayload().toString());
                Hello hello = objectMapper.readValue(json, Hello.class);
                String token = hello.getToken();
                TokenData tokenData = jwtAuthorizationFilter.validateToken(token);
                if (tokenData.getErrorText() != null) {
                    session.sendMessage(new TextMessage(ErrorMessage.makeError(String.format("Token error. %s. Closing connection.", tokenData.getErrorText()))));
                    session.close();
                    return;
                }
                if (tokenData.getUsername() == null) {
                    session.sendMessage(new TextMessage(ErrorMessage.makeError("No username")));
                    session.close();
                    return;
                }
                setUsernameIdForWSSession(session, tokenData.getUsername());
                break;
            case "ACTION":
                Action action = objectMapper.readValue(json, Action.class);
                logger.info("sending message " + action.getAction());
                eventPublisher.publishEvent(new WSEvent(wsTextMessage));
                // do action
                // if
//                Controller controller = сontrollerService.findController(action.getMac());
//                if (controller != null) {
//                    if ("relaycontroller".equals(controller.getType())) {
//
//                    }
//                }
                break;

            default:
                logger.warn("Unknown message");
                logger.info(wsTextMessage.getPayload().toString());
                session.sendMessage(new TextMessage(AlertMessage.makeAlert("I receive your message, but nothing to say...")));
                break;
        }

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        wsSessions.add(new WSUISession(session));
        logger.info(String.format("New webclient connected with ID %s", session.getId()));
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        for (WSUISession wsSession : wsSessions) {
            if (wsSession.getSession().equals(session)) {
                wsSessions.remove(wsSession);
                break;
            }
        }
        logger.info(String.format("WebClient disconnected with ID %s", session.getId()));
        super.afterConnectionClosed(session, status);
    }

    private void setUsernameIdForWSSession(WebSocketSession targetSession, String username) {
        for (WSUISession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setUsername(username);
                break;
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    private void test() throws IOException {
        RCUpdateMessage rcUpdateMessage = new RCUpdateMessage();
        rcUpdateMessage.setMac("30AEA48662E0");
        Random rnd = new Random();
        rcUpdateMessage.setOutput(rnd.nextInt(8));
        rcUpdateMessage.setState(rnd.nextBoolean() ? "on" : "off");

        for (WSUISession wsSession : wsSessions) {
            wsSession.getSession().sendMessage(new TextMessage(rcUpdateMessage.makeMessage()));
            //"{\"type\": \"test\", \"payload\": {\"id\": 123}}"
        }
    }

}
