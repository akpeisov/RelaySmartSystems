package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.filters.IpHandshakeInterceptor;
import kz.home.RelaySmartSystems.filters.JwtAuthorizationFilter;
import kz.home.RelaySmartSystems.model.*;
import kz.home.RelaySmartSystems.model.def.Action;
import kz.home.RelaySmartSystems.model.def.Hello;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.model.relaycontroller.RCUpdate;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

// класс для контроллеров
@Component // иначе из конфигурации не привяжется класс
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ArrayList<WSSession> wsSessions = new ArrayList<WSSession>();
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ControllerService сontrollerService;
    private final RelayControllerService relayControllerService;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final UserService userService;

    public WebSocketHandler(ControllerService сontrollerService,
                            RelayControllerService relayControllerService, JwtAuthorizationFilter jwtAuthorizationFilter, UserService userService) {
        this.сontrollerService = сontrollerService;
        this.relayControllerService = relayControllerService;
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
        this.userService = userService;
    }

//    @Override
//    public void onApplicationEvent(WSEvent event) {
//        // Обработка события
//        try {
//            WSTextMessage wsTextMessage = (WSTextMessage) event.getSource();
//            if ("ACTION".equals(wsTextMessage.getType())) {
//                // это действие, надо найти нужную сессию и отправить туда
//                Action action = (Action)wsTextMessage.getPayload();
//                String mac = action.getMac();
//                sendMessageToUser(mac, wsTextMessage.makeMessage());
//            }
//        } catch (Exception e) {
//
//        }
//    }

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
            session.sendMessage(new TextMessage(AlertMessage.makeAlert("WS session not found")));
            session.close();
            return;
        }

        String payload = message.getPayload();
        logger.info(payload);

        byte[] json = null;
        WSTextMessage wsTextMessage = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wsTextMessage = objectMapper.readValue(payload, WSTextMessage.class);
            json = objectMapper.writeValueAsBytes(wsTextMessage.getPayload());
        } catch (Exception e) {
            session.sendMessage(new TextMessage(AlertMessage.makeAlert("Wrong message type")));
            session.close();
            return;
        }

        String type = wsTextMessage.getType();
        if (type == null) {
            return;
        }
//        if (!"HELLO".equals(type) && wsSession.getControllerId() == null) {
//            session.sendMessage(new TextMessage(AlertMessage.makeAlert("No session identifier present")));
//            session.close();
//            return;
//        }

        switch (type) {
            case "HELLO":
                logger.info(wsTextMessage.getPayload().toString());
                Hello hello = objectMapper.readValue(json, Hello.class);
                String token = hello.getToken();
                TokenData tokenData = jwtAuthorizationFilter.validateToken(token, hello.getType());
                if (tokenData.getErrorText() != null) {
                    session.sendMessage(new TextMessage(ErrorMessage.makeError(String.format("Token error. %s. Closing connection.", tokenData.getErrorText()))));
                    session.close();
                    return;
                }
                if (tokenData.getMac() == null && tokenData.getUsername() == null) {
                    session.sendMessage(new TextMessage(ErrorMessage.makeError("No identifiers!")));
                    session.close();
                    return;
                }
                // Это клиент фронта
                if (tokenData.getUsername() != null) {
                    setUsernameForWSSession(session, tokenData.getUsername());
                    User user = userService.findById(tokenData.getUsername()).orElse(null);
                    if (user != null)
                        setUserForWSSession(session, user);
                    else {
                        logger.info(String.format("User by username not found %s", tokenData.getUsername()));
                        session.close();
                    }
                }
                setTypeForWSSession(session, hello.getType());
                // если новое подключение с тем же маком, то надо поискать старые сессии и удалить старые
                // если это устройство, то у него есть мак
                if (tokenData.getMac() != null) {
                    setControllerIdForWSSession(session, tokenData.getMac());

                    WSSession sessionToDel = null;
                    for (WSSession wsSessionOld : wsSessions) {
                        if (tokenData.getMac().equalsIgnoreCase(wsSessionOld.getControllerId()) && !wsSessionOld.getSession().getId().equals(session.getId())) {
                            logger.info(String.format("Removed mac %s from other session %s", tokenData.getMac(), wsSessionOld.getSession().getId()));
                            //wsSessions.remove(wsSessionOld); // java.util.ConcurrentModificationException: null Нельзя удалять из коллекции при итерировании
                            sessionToDel = wsSessionOld;
                            wsSessionOld.getSession().close();
                            break;
                        }
                    }
                    if (sessionToDel != null)
                        wsSessions.remove(sessionToDel);

                    сontrollerService.setControllerStatus(tokenData.getMac(), "online");
                    logger.info(String.format("isControllerLinked %s %s", tokenData.getMac(), isControllerLinked(tokenData.getMac()))); ;
                    if (!isControllerLinked(tokenData.getMac())) {
                        сontrollerService.addController(tokenData.getMac().toUpperCase(), hello.getType());
                        logger.info("Sending message INFO");
                        session.sendMessage(new TextMessage(AlertMessage.makeAlert("INFO")));
                        // заполнить таблицу controllers для возможности линковки
                    } else {
                        session.sendMessage(new TextMessage(AlertMessage.makeAlert("READY")));
                        setUserForWSSession(session, сontrollerService.getUserByController(tokenData.getMac()));
                    }
                }
                break;
            case "INFO":
                logger.info(wsTextMessage.getPayload().toString());
                //Info info = (Info)wsTextMessage.getPayload();
                Info info = objectMapper.readValue(json, Info.class);
                сontrollerService.setControllerInfo(info);
//                if ("relaycontroller".equals(wsSession.getType())) {
//                    relayControllerService.setRelayControllerInfo(info);
//                }
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
                    //relayController.setStatus("online");
                    relayControllerService.addRelayController(relayController, user);
                }
                break;
            case "UPDATE":
                if ("relaycontroller".equals(wsSession.getType())) {
                    RCUpdate update = objectMapper.readValue(json, RCUpdate.class);
                    //RCEvent event = objectMapper.readValue(json, RCEvent.class);
                    if (update.getOutput() != null) {
                        relayControllerService.setOutputState(wsSession.getControllerId(), update.getOutput(), update.getState());
                    } else if (update.getInput() != null) {
                        relayControllerService.setInputState(wsSession.getControllerId(), update.getInput(), update.getState());
                    }
                    // найти пользователя, у которого есть этот контроллер, затем найти все его сессии и отправить туда
                    User user = relayControllerService.getUser(wsSession.getControllerId());
                    if (user != null) {
                        logger.info(String.format("User found %s", user.getFio()));
                        SendMessageToWebUser(user, wsTextMessage.makeMessage());
                    } else {
                        logger.error("User not found");
                    }
                }
                break;
            case "ACTION":
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    Action action = objectMapper.readValue(json, Action.class);
                    logger.info(String.format("Sending message %s to controller %s", action.getAction(), action.getMac()));
                    // это действие, надо найти нужную сессию и отправить туда
                    SendMessageToController(action.getMac(), wsTextMessage.makeMessage());
                }
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
        WSSession wsSession = new WSSession(session);
        wsSession.setConnectionDate(LocalDateTime.now());
        String clientIpAddress = (String) session.getAttributes().get(IpHandshakeInterceptor.CLIENT_IP_ADDRESS_KEY);
        wsSession.setClientIP(clientIpAddress == null ? session.getRemoteAddress().toString() : clientIpAddress);
        wsSessions.add(wsSession);
        logger.info(String.format("New client connected with ID %s %s %s", session.getId(), session.getRemoteAddress(), clientIpAddress));
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // find current conrtoller and set offline
        String mac = getControllerIdForWSSession(session);
        if (mac != null)
            сontrollerService.setControllerStatus(mac, "offline");
        String type = "";
        WSSession sessionToDel = null;
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(session)) {
                type = wsSession.getType();
                sessionToDel = wsSession;
                //wsSessions.remove(wsSession);
                break;
            }
        }
        if (sessionToDel != null)
            wsSessions.remove(sessionToDel);

        //wsSessions.remove(new WSSession(session));
        logger.info(String.format("Client %s disconnected with ID %s", type, session.getId()));
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.info(String.format("handleTransportError with ID %s %s", session.getId(), exception.getMessage()));
    }

    public String SendMessageToController(String controllerId, String message) {
        if (controllerId == null)
            return "MAC_NULL";

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

    public void SendMessageToWebUser(User user, String message) {
        // Отправка сообщения во все пользовательские сессии
        if (user == null)
            return;

        for (WSSession session: wsSessions) {
            if ("web".equalsIgnoreCase(session.getType()) && user.equals(session.getUser())) {
                try {
                    if (session.getSession().isOpen()) {
                        session.getSession().sendMessage(new TextMessage(message));
                        logger.info(String.format("Message to user %s sent", user.getFio()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setControllerIdForWSSession(WebSocketSession targetSession, String controllerId) {
        if (controllerId == null)
            return;
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setControllerId(controllerId.toUpperCase());
                break;
            }
        }
    }

    private void setUsernameForWSSession(WebSocketSession targetSession, String username) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                wsSession.setUsername(username);
                break;
            }
        }
    }

    private void setTypeForWSSession(WebSocketSession targetSession, String type) {
        if (type == null)
            return;
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
        SendMessageToController(mac, getJsonRequestConfig());
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
            return SendMessageToController(mac, json);
        } catch (JsonProcessingException e) {
            //throw new RuntimeException(e);
        }
        return "ERROR";
    }

//    @Scheduled(fixedRate = 5000)
    private void test() throws IOException {
        RCUpdate rcUpdateMessage = new RCUpdate();
        rcUpdateMessage.setMac("30AEA48662E0");
        Random rnd = new Random();
        rcUpdateMessage.setOutput(rnd.nextInt(8));
        rcUpdateMessage.setState(rnd.nextBoolean() ? "on" : "off");

        for (WSSession wsSession : wsSessions) {
            if (wsSession.getUsername() != null)
                wsSession.getSession().sendMessage(new TextMessage(rcUpdateMessage.makeMessage()));
            //"{\"type\": \"test\", \"payload\": {\"id\": 123}}"
        }
    }
    @Scheduled(fixedRate = 5000)
    private void alive() throws IOException {
        for (WSSession wsSession : wsSessions) {
            if ("web".equalsIgnoreCase(wsSession.getType())) {
                wsSession.getSession().sendMessage(new TextMessage(AlertMessage.makeAlert("alive")));
            }
        }
    }

    public List<WSSession> getCurrentWSSessions() {
        return wsSessions;
    }
}
