package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.home.RelaySmartSystems.filters.IpHandshakeInterceptor;
import kz.home.RelaySmartSystems.filters.JwtAuthorizationFilter;
import kz.home.RelaySmartSystems.model.*;
import kz.home.RelaySmartSystems.model.def.Action;
import kz.home.RelaySmartSystems.model.def.Hello;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.model.def.LinkRequest;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
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
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

// класс для контроллеров
@Component // иначе из конфигурации не привяжется класс
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ArrayList<WSSession> wsSessions = new ArrayList<WSSession>();
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ControllerService controllerService;
    private final RelayControllerService relayControllerService;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final UserService userService;

    public WebSocketHandler(ControllerService controllerService,
                            RelayControllerService relayControllerService, JwtAuthorizationFilter jwtAuthorizationFilter, UserService userService) {
        this.controllerService = controllerService;
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
        // Ищем текущую сессию в списке сессий
        WSSession wsSession = null;
        for (WSSession wsSession1 : wsSessions) {
            if (wsSession1 != null && wsSession1.getSession().equals(session)) {
                wsSession = wsSession1;
                break;
            }
        }
        if (wsSession == null) {
            logger.error("wsSession not found!");
            //session.sendMessage(new TextMessage(AlertMessage.makeAlert("WS session not found")));
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
            session.sendMessage(new TextMessage(errorMessage("Wrong message type")));
            session.close();
            return;
        }

        String type = wsTextMessage.getType();
        if (type == null) {
            session.sendMessage(new TextMessage(errorMessage("Type is null")));
            session.close();
            return;
        }

        if (!wsSession.isAuthorized() && !type.equals("HELLO") && !type.equals("DEVICECONFIG")) {
            session.sendMessage(new TextMessage(errorMessage("Good bye!")));
            session.close();
            return;
        }


        // TODO : разделить запросы и ответы от контроллеров и фронта (только хэллоу общий)


        switch (type) {
            case "HELLO":
                // Первый метод для авторизации устройства или фронта. Проверка авторизации.
                // У фронта есть Username, у устройства есть Mac
                Hello hello = objectMapper.readValue(json, Hello.class);
                String token = hello.getToken();
                TokenData tokenData = jwtAuthorizationFilter.validateToken(token, hello.getType());
                if (tokenData.getErrorText() != null) {
                    logger.error(String.format("Token error %s", tokenData.getErrorText()));
                    session.sendMessage(new TextMessage(errorMessage(String.format("Token error. %s. Closing connection.", tokenData.getErrorText()))));
                    session.close();
                    break;
                }
                wsSession.setType(hello.getType());

                if (tokenData.getMac() != null) {
                    // Это контроллер
                    String mac = tokenData.getMac();
                    wsSession.setControllerId(mac);
                    // если контроллера нет, то запросить его конфиг
                    if (controllerService.findController(mac) == null) {
                        logger.info("Controller {} not found. Request config", mac);
                        session.sendMessage(new TextMessage(getCmdMessage("GETDEVICECONFIG")));
                        break;
                    } else {
                        if (controllerService.isControllerLinked(mac)) {
                            wsSession.setUser(controllerService.findControllerOwner(mac));
                        }
                    }
                } else if (tokenData.getUsername() != null) {
                    // Это клиент фронта
                    //setUsernameForWSSession(session, tokenData.getUsername());
                    wsSession.setUsername(tokenData.getUsername());
                    User user = userService.findById(tokenData.getUsername()).orElse(null);
                    if (user != null) {
                        wsSession.setUser(user);
                        //setUserForWSSession(session, user);
                    } else {
                        logger.info(String.format("User by username not found %s", tokenData.getUsername()));
                        session.sendMessage(new TextMessage(errorMessage("User not found!")));
                        session.close();
                        break;
                    }
                } else {
                    session.sendMessage(new TextMessage(errorMessage("No identifiers!")));
                    session.close();
                    break;
                }
                //setTypeForWSSession(session, hello.getType());
                wsSession.setAuthorized(true);
                session.sendMessage(new TextMessage(getCmdMessage("AUTHORIZED")));
                break;

//            case "CHECK":
//                // проверить наличие контроллера
//                if (controllerService.findController(wsSession.getControllerId()) == null) {
//                    logger.info(String.format("controller %s not found. Trying to add it",
//                            wsSession.getControllerId()));
//                    if ("relaycontroller".equalsIgnoreCase(wsSession.getType())) {
//
//                    }
//                    //controllerService.addController(wsSession.getControllerId().toUpperCase(), wsSession.getType());
//                    // запросить конфиг устройства
//                    session.sendMessage(new TextMessage(getCmdMessage("GETDEVICECONFIG")));
//                } else {
//                    // существует
//                    session.sendMessage(new TextMessage(getCmdMessage("READY")));
//                }
//                if (controllerService.isControllerLinked(wsSession.getControllerId())) {
//                    wsSession.setUser(controllerService.findControllerOwner(wsSession.getControllerId()));
//                }
//                controllerService.setControllerOnline(wsSession.getControllerId());
//                break;

            case "DEVICECONFIG":
                // получение конфига от контроллера
                // вызывается если это новый контроллер
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    //User user = controllerService.getUserByController(wsSession.getControllerId());
                    RelayController relayController = objectMapper.readValue(json, RelayController.class);
                    relayController.setMac(wsSession.getControllerId());
                    relayControllerService.addRelayController(relayController, null);
                    wsSession.setAuthorized(true);
                    session.sendMessage(new TextMessage(getCmdMessage("AUTHORIZED")));
                } else {
                    session.sendMessage(new TextMessage(errorMessage("Unknown type")));
                }
                break;

            case "INFO":
                // получение информации о контроллере (аптайм, и т.д.)
                if (!"web".equalsIgnoreCase(wsSession.getType())) {
                    Info info = objectMapper.readValue(json, Info.class);
                    controllerService.setControllerInfo(info);
                    // send to web
                    WSTextMessage wsMsg = new WSTextMessage("INFO", info);
                    logger.warn(wsMsg.makeMessage());
                    //sendMessageToWebUser(wsSession.getUser(), wsMsg.makeMessage());
//                    logger.warn(payload);
//                    logger.warn(info.getJson());
                }
                break;

            case "IOSTATES":
                // Обновление состояния входов/выходов сразу всех. Используется при подключении контроллера
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    RelayController relayController = objectMapper.readValue(json, RelayController.class);
                    relayController.setMac(wsSession.getControllerId());
                    relayControllerService.updateRelayControllerIOStates(relayController);
                }
                break;

            case "UPDATE":
                // обновление состояния входов/выходов в базе и отправка на фронт
//            {"type":"UPDATE","payload":{"mac":"C8F09E311008","input":16,"state":"long"}}
                // проверить линковку
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    RCUpdate update = objectMapper.readValue(json, RCUpdate.class);
                    if (update.getOutput() != null) {
                        relayControllerService.setOutputState(wsSession.getControllerId(), update.getOutput(), update.getState(), update.getSlaveId());
                    } else if (update.getInput() != null) {
                        if ((update.getInput() == 16) && ("long".equalsIgnoreCase(update.getState()))) {
                            // event for link
                            // проверить есть ли запрос на линковку в веб сессии
                            WSSession linkSession = findSessionForLinkRequest(update.getMac());
                            //User user = isLinkRequestActive(update.getMac());
                            if (linkSession != null) {
                                logger.info("Link request ok");
                                if (controllerService.linkController(update.getMac(), linkSession.getUser()).equalsIgnoreCase("OK")) {
                                    linkSession.getSession().sendMessage(new TextMessage(WSTextMessage.send("LINKOK", null)));
                                }
                            }
                        } else {
                            relayControllerService.setInputState(wsSession.getControllerId(), update.getInput(), update.getState());
                        }
                    }
                    // отправить уведомление владельцу контроллера
                    if (wsSession.getUser() != null) {
                        //logger.info(String.format("User found %s", user.getFio()));
                        sendMessageToWebUser(wsSession.getUser(), wsTextMessage.makeMessage());
                    } else {
                        logger.info("no user found");
                    }
                }
                break;

            case "SETDEVICECONFIG":
                // результат обновления конфига от контроллера
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    Message message1 = objectMapper.readValue(json, Message.class);
                    if (message1.getMessage() != null) {
                        logger.info(String.format("SETDEVICECONFIG msg %s", message1.getMessage()));
                        //session.sendMessage(new TextMessage());
                        if ("OK".equalsIgnoreCase(message1.getMessage())) {
                            sendMessageToWebUser(wsSession.getUser(), successMessage("Upload successful"));
                        } else {
                            sendMessageToWebUser(wsSession.getUser(), errorMessage("Upload to controller failed"));
                        }
                    }
                }
                break;

            case "ACTION":
                // отправка действия на контроллер
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    Action action = objectMapper.readValue(json, Action.class);
                    // TODO : security check for controller's owner is correct
                    logger.info(String.format("Sending message %s to controller %s", action.getAction(), action.getMac()));
                    // это действие, надо найти нужную сессию и отправить туда
                    sendMessageToController(action.getMac(), wsTextMessage.makeMessage());
                }
                break;

            case "UPDATEOUTPUT":
                // обновление выхода контроллера
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    RCUpdateOutput rcUpdateOutput = objectMapper.readValue(json, RCUpdateOutput.class);
                    String res = sendMessageToController(rcUpdateOutput.getMac(), wsTextMessage.makeMessage());
                    if (!"OK".equalsIgnoreCase(res)) {
                        session.sendMessage(new TextMessage(errorMessage(res)));
                    }
                    // пока только для relaycontroller
                    relayControllerService.updateOutput(rcUpdateOutput);
                }
                break;

            case "UPDATEINPUT":
                // обновление входа контроллера
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    // пока только для relaycontroller
                    RCUpdateInput rcUpdateInput = objectMapper.readValue(json, RCUpdateInput.class);
                    if (rcUpdateInput.getUuid() != null) {
                        String res = relayControllerService.updateInput(rcUpdateInput);
                        if ("OK".equalsIgnoreCase(res)) {
                            //logger.info(correctJSON(wsTextMessage.makeMessage()));
                            //SendMessageToController(rcUpdateInput.getMac(), wsTextMessage.makeMessage());
                            session.sendMessage(new TextMessage(successMessage("Saved successfully")));
                        } else {
                            session.sendMessage(new TextMessage(errorMessage(res)));
                        }
                    } else {
                        session.sendMessage(new TextMessage(errorMessage("Can't parse request")));
                    }

                }
                break;

            case "REQUESTFORLINK":
                // запрос на линк контроллера
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    LinkRequest linkRequest = objectMapper.readValue(json, LinkRequest.class);
                    if (linkRequest.getMac() != null) {
                        Controller controller = controllerService.findController(linkRequest.getMac());
                        if (controller != null) {
                            // проверить готов ли контроллер к линку
                            if (controller.isLinked()) {
                                session.sendMessage(new TextMessage(errorMessage("Controller already linked")));
                            } else {
                                //session.sendMessage(new TextMessage(AlertMessage.makeAlert(controller.getType())));
                                // проверить онлайн ли сейчас контроллер
                                if (!isControllerOnline(linkRequest.getMac())) {
                                    session.sendMessage(new TextMessage(errorMessage("Controller is offline. Make sure that is powered on and connect to internet.")));
                                } else {
                                    Map<String, Object> payld = new HashMap<>();
                                    //payld.put("message", 123);
                                    payld.put("controllertype", controller.getType());
                                    session.sendMessage(new TextMessage(WSTextMessage.send("LINK", payld)));
                                    // find controller and wait link event from it
                                    // make temporary flag
                                    wsSession.setControllerId(linkRequest.getMac());
                                    wsSession.setObj("linkRequested"); // set curtime
                                }
                            }
                        } else {
                            session.sendMessage(new TextMessage(errorMessage("Controller not found")));
                        }
                    } else if ("linkRequestTimeout".equalsIgnoreCase(linkRequest.getEvent())) {
                        wsSession.setObj(null);
                    } else {
                        session.sendMessage(new TextMessage(errorMessage("Controller not found")));
                    }
                }
                break;

            case "UPLOADCONFIG":
                // команда отправки конфига на контроллер
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    Controller controller = objectMapper.readValue(json, Controller.class);
                    if ("test".equalsIgnoreCase(controller.getMac())) {
                        //session.sendMessage(new TextMessage("OK"));
                        //session.sendMessage(new TextMessage(errorMessage("Test")));
                        session.sendMessage(new TextMessage(successMessage("Test")));
                    }
                    else if (controller.getMac() != null) {
                        if (isControllerOnline(controller.getMac())) {
                            // make config and send to controller
                            String res = sendMessageToController(controller.getMac(), relayControllerService.makeDeviceConfig(controller.getMac()));
                            //logger.info("len " +relayControllerService.makeDeviceConfig(controller.getMac()).getBytes().length);
                            //String res = sendMessageToController(controller.getMac(), "{\"type\":\"INFO\"}");
                            //String res = sendMessageToController(controller.getMac(), "{\"type\":\"INFO\", \"payload\": \"" +genTest(5500)+ "\"}");

                            logger.info(String.format("sendMessageToController %s", res));
                            if (!"OK".equalsIgnoreCase(res)) {
                                session.sendMessage(new TextMessage(errorMessage(res)));
                            }
                        } else {
                            session.sendMessage(new TextMessage(errorMessage("Controller offline")));
                        }
                    }
                }
                break;

            case "TEST":
                // команда отправки конфига на контроллер
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    Controller controller = objectMapper.readValue(json, Controller.class);

                }
                break;

            default:
                logger.warn(String.format("Unknown message %s", type));
                logger.info(wsTextMessage.getPayload().toString());
                session.sendMessage(new TextMessage(errorMessage("I receive your message, but nothing to say...")));
                break;
        }
    }

    private String genTest(int len) {
        String SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder result = new StringBuilder();

        while (result.length() < len) {
            // Добавить случайный символ
            char c = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
            result.append(c);

        }
        //return "{\"test\": \"" + result.substring(0, len) + "\"}";
        return result.substring(0, len);
    }

    private String errorMessage(String message) {
        Map<String, Object> payld = new HashMap<>();
        payld.put("message", message);
        return WSTextMessage.send("ERROR", payld);
    }

    private String successMessage(String message) {
        Map<String, Object> payld = new HashMap<>();
        payld.put("message", message);
        return WSTextMessage.send("SUCCESS", payld);
    }

    private String getCmdMessage(String cmd) {
        return WSTextMessage.send(cmd, null);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WSSession wsSession = new WSSession(session);
        wsSession.setConnectionDate(LocalDateTime.now());
        String clientIpAddress = (String) session.getAttributes().get(IpHandshakeInterceptor.CLIENT_IP_ADDRESS_KEY);
        wsSession.setClientIP(clientIpAddress == null ? session.getRemoteAddress().toString() : clientIpAddress);
        wsSessions.add(wsSession);
        logger.info(String.format("New client connected with ID %s remote IP %s client IP %s", session.getId(), session.getRemoteAddress(), clientIpAddress));
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // find current conrtoller and set offline
        String mac = getControllerIdForWSSession(session);
        if (mac != null)
            controllerService.setControllerOffline(mac);
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

    public String sendMessageToController(String controllerId, String message) {
        if (controllerId == null)
            return "MAC_NULL";

        for (WSSession session: wsSessions) {
            if (!"web".equalsIgnoreCase(session.getType()) &&
                    controllerId.toUpperCase().equals(session.getControllerId())) {
                try {
                    if (session.getSession().isOpen()) {
                        session.getSession().sendMessage(new TextMessage(message));
                        return "OK";
                    }
                } catch (IOException e) {
                    logger.error(e.getLocalizedMessage());
                    return "ERROR";
                }
                break;
            }
        }
        return "NOT_FOUND";
    }

    public void sendMessageToWebUser(User user, String message) {
        // Отправка сообщения во все пользовательские сессии
        logger.info(String.format("sendMessageToWebUser User %s. Message %s", user.getId(), message));
        if (user.getId() == null)
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

    private WSSession findSessionForLinkRequest(String mac) {
        // определить есть ли запрос на линковку в текущих сессиях фронта
        for (WSSession wsSession : wsSessions) {
            if ("web".equals(wsSession.getType()) &&
                    wsSession.getControllerId() != null &&
                    "linkRequested".equalsIgnoreCase((String)wsSession.getObj()))  {
                return wsSession;
            }
        }
        return null;
    }

    private boolean isControllerOnline(String mac) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getControllerId() != null && wsSession.getControllerId().equals(mac)) {
                return true;
            }
        }
        return false;
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
        // установить владельца для сессии устройства
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
        return controllerService.isControllerLinked(mac);
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
            if (wsSession != null && "web".equalsIgnoreCase(wsSession.getType()) && wsSession.getSession().isOpen()) {
                String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
                wsSession.getSession().sendMessage(new TextMessage(AlertMessage.makeAlert(String.format("alive %s", date))));
            }
        }
    }

    @Scheduled(fixedRate = 20000)
    private void serviceTask() {
        controllerService.setOffline();
    }

    public List<WSSession> getCurrentWSSessions() {
        return wsSessions;
    }

    private String correctJSON(String json) {
        // TODO : перенести в отдельный статичный класс
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        String cleanedJson = null;
        try {
            root = mapper.readTree(json);
            removeFieldsRecursive(root, "uuid", "outputID", "order", "state");
            cleanedJson = mapper.writer().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return cleanedJson;
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = null;
//        String result = null;
//        try {
//            jsonNode = objectMapper.readTree(json);
//            ObjectNode object = (ObjectNode) jsonNode;
//            object.remove("uuid");
//            result = objectMapper.writeValueAsString(object);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return result;
//        Assertions.assertEquals("{\"name\":\"John\",\"city\":\"New York\"}", updatedJson);
    }


    private static void removeFieldsRecursive(JsonNode node, String... fieldsToRemove) {
        // TODO : перенести в отдельный статичный класс
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            for (String field : fieldsToRemove) {
                objNode.remove(field);
            }
            Iterator<Map.Entry<String, JsonNode>> iter = objNode.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                removeFieldsRecursive(entry.getValue(), fieldsToRemove);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                removeFieldsRecursive(item, fieldsToRemove);
            }
        }
    }
}
