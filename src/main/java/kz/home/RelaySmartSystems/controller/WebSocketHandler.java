package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.Controller;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.WSSession;
import kz.home.RelaySmartSystems.model.WSTextMessage;
import kz.home.RelaySmartSystems.model.def.Hello;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.model.relaycontroller.RCEvent;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component // иначе из конфигурации не привяжется класс
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ArrayList<WSSession> wsSessions = new ArrayList<WSSession>();
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ControllerService сontrollerService;
    private final RelayControllerService relayControllerService;

    public WebSocketHandler(ControllerService сontrollerService,
                            RelayControllerService relayControllerService) {
        this.сontrollerService = сontrollerService;
        this.relayControllerService = relayControllerService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // find current wsSession for session
        WSSession wsSession = null;
        for (WSSession wsSession1 : wsSessions) {
            if (wsSession1.getSession().equals(session)) {
                wsSession = wsSession1;
                break;
            }
        }
        if (wsSession == null) {
            logger.error("wsSession not found!");
            return;
        }

        String payload = message.getPayload();
        logger.info(payload);

        // TODO : add catch for incorrect data
        ObjectMapper objectMapper = new ObjectMapper();
        WSTextMessage wsTextMessage = objectMapper.readValue(payload, WSTextMessage.class);
        byte[] json = objectMapper.writeValueAsBytes(wsTextMessage.getPayload());

        String type = wsTextMessage.getType();
        if (type == null) {
            return;
        }

        switch (type) {
            case "HELLO":
                logger.info(wsTextMessage.getPayload().toString());
                Hello hello = objectMapper.readValue(json, Hello.class);
                String mac = hello.getMac();
                setControllerIdForWSSession(session, mac);
                setTypeForWSSession(session, hello.getType());
                logger.info(String.format("isControllerLinked %s", isControllerLinked(mac))); ;
                if (!isControllerLinked(mac)) {
                    сontrollerService.addController(mac.toUpperCase(), hello.getType());
                    // попросить конфиг и заполнить таблицу
                    // просить конфиг надо при привязке к юзеру???
                    //sendMessageToUser(mac, getJsonRequestConfig());
                } else {
                    setUserForWSSession(session, сontrollerService.getUserByController(mac));
                    if ("relaycontroller".equals(hello.getType())) {
                        relayControllerService.setRelayControllerStatus(mac, "online");
                    }
                }
                break;
            case "INFO":
                logger.info(wsTextMessage.getPayload().toString());
                //Info info = (Info)wsTextMessage.getPayload();
                Info info = objectMapper.readValue(json, Info.class);
                if ("relaycontroller".equals(wsSession.getType())) {
                    relayControllerService.setRelayControllerInfo(info);
                }

                //setControllerIdForSession(session, info.getMac());
                break;
            case "DEVICECONFIG":
                logger.info(wsTextMessage.getPayload().toString());
                if ("relaycontroller".equals(wsSession.getType())) {
                    User user = wsSession.getUser();
                    if (user == null) {
                        // может быть так что это сессия нового устройства, которое только прилинковали.
                        // В этом случае попробуем найти пользователя
                        user = сontrollerService.getUserByController(wsSession.getControllerId());
                        if (user == null) {
                            logger.error("DEVICECONFIG. No user found");
                            return;
                        }
                    }
                    RelayController relayController = objectMapper.readValue(json, RelayController.class);
                    relayController.setMac(wsSession.getControllerId());
                    relayController.setStatus("online");
                    relayControllerService.addRelayController(relayController, user);
                }
                break;
            case "EVENT":
                if ("relaycontroller".equals(wsSession.getType())) {
                    RCEvent event = objectMapper.readValue(json, RCEvent.class);
                    if (event.getOutput() != null) {
                        relayControllerService.setOutputState(wsSession.getControllerId(), event.getOutput(), event.getEvent());
                    } else if (event.getInput() != null) {
                        relayControllerService.setInputState(wsSession.getControllerId(), event.getInput(), event.getEvent());
                    }
                }
                break;
            default:
                logger.info(wsTextMessage.getPayload().toString());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        wsSessions.add(new WSSession(session));
        logger.info(String.format("New client connected with ID %s", session.getId()));
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // find current conrtoller and set offline
        String mac = getControllerIdForWSSession(session);
        setControllerOffline(mac);

        wsSessions.remove(new WSSession(session));
        logger.info(String.format("Client disconnected with ID %s", session.getId()));
        super.afterConnectionClosed(session, status);
    }

    public String sendMessageToUser(String controllerId, String message) {
        if (controllerId == null)
            return "ERROR";

        for (WSSession session: wsSessions) {
            if (controllerId.toUpperCase().equals(session.getControllerId())) {
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

    private void setControllerIdForWSSession(WebSocketSession targetSession, String controllerId) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setControllerId(controllerId.toUpperCase());
                break;
            }
        }
    }

    private void setTypeForWSSession(WebSocketSession targetSession, String type) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setType(type);
                break;
            }
        }
    }

    private void setUserForWSSession(WebSocketSession targetSession, User user) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setUser(user);
                break;
            }
        }
    }

    private String getControllerIdForWSSession(WebSocketSession targetSession) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                return wsSession.getControllerId();
            }
        }
        return null;
    }
    private String getTypeForWSSession(WebSocketSession targetSession) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                return wsSession.getType();
            }
        }
        return null;
    }

    private User getUserForWSSession(WebSocketSession targetSession) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                return wsSession.getUser();
            }
        }
        return null;
    }

    boolean isControllerLinked(String mac) {
        // вернет true если контроллер привязан к пользователю
        return сontrollerService.isControllerLinked(mac);
    }

    private String getJsonRequestConfig() {
        return "{\"type\": \"GETDEVICECONFIG\"}";
    }

    public void requestControllerConfig(String mac) {
        sendMessageToUser(mac, getJsonRequestConfig());
    }

    void setControllerOffline(String mac) {
        Controller c = сontrollerService.findController(mac);
        if (c != null) {
            if ("relaycontroller".equals(c.getType())) {
                relayControllerService.setRelayControllerStatus(mac, "offline");
            }
            // TODO : add other controllers types
        }
    }

    public String sendDeviceAction(String mac, Integer output, String action, Integer slaveId) {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "ACTION");
        objectMap.put("payload", new HashMap<String, Object>() {{
            put("output", output);
            put("action", action);
            if (slaveId > 0)
                put("slaveid", slaveId);
        }});
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(objectMap);
            logger.info(json);
            return sendMessageToUser(mac, json);
        } catch (JsonProcessingException e) {
            //throw new RuntimeException(e);
        }
        return "ERROR";
    }
}
