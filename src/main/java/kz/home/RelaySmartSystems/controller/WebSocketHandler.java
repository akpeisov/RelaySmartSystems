package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.filters.IpHandshakeInterceptor;
import kz.home.RelaySmartSystems.filters.JwtAuthorizationFilter;
import kz.home.RelaySmartSystems.model.*;
import kz.home.RelaySmartSystems.model.def.*;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.SessionService;
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
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private static final ArrayList<WSSession> wsSessions = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ControllerService controllerService;
    private final RelayControllerService relayControllerService;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;
    private final UserService userService;
    private final SessionService sessionService;

    public WebSocketHandler(ControllerService controllerService,
                            RelayControllerService relayControllerService,
                            JwtAuthorizationFilter jwtAuthorizationFilter,
                            UserService userService,
                            SessionService sessionService) {
        this.controllerService = controllerService;
        this.relayControllerService = relayControllerService;
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
        this.userService = userService;
        this.sessionService = sessionService;

        sessionService.endAllSessions();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        sessionService.updateLastActive(session.getId());
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
            session.close();
            return;
        }

        String payload = message.getPayload();
        logger.info(payload);

        byte[] json;
        WSTextMessage wsTextMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            wsTextMessage = objectMapper.readValue(payload, WSTextMessage.class);
            json = objectMapper.writeValueAsBytes(wsTextMessage.getPayload());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
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
            session.close(CloseStatus.TLS_HANDSHAKE_FAILURE); // :)
            return;
        }


        // TODO : разделить запросы и ответы от контроллеров и фронта (только хэллоу общий)
        String res;

        switch (type) {
            case "HELLO":
                // Первый метод для авторизации устройства или фронта. Проверка авторизации.
                // У фронта есть Username, у устройства есть Mac
                Hello hello = objectMapper.readValue(json, Hello.class);
                String token = hello.getToken();
                TokenData tokenData = jwtAuthorizationFilter.validateToken(token, hello.getType());
                if (tokenData.getErrorText() != null) {
                    logger.error("Token error {}", tokenData.getErrorText());
                    session.sendMessage(new TextMessage(errorMessage(String.format("Token error. %s. Closing connection.", tokenData.getErrorText()))));
                    session.close(CloseStatus.GOING_AWAY);
                    break;
                }
                wsSession.setType(hello.getType());
    // TODO : убрать после теста
    if (hello.getType().equalsIgnoreCase("WEB1"))
        wsSession.setType("WEB");
    else if (hello.getType().equalsIgnoreCase("RC1"))
        wsSession.setType("relayController");

                if (tokenData.getMac() != null) {
                    // Это контроллер
                    String mac = tokenData.getMac();
                    wsSession.setControllerId(mac);
                    sessionService.setMac(session.getId(), mac);
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
                    sessionService.setUsername(session.getId(), tokenData.getUsername());
                    wsSession.setUsername(tokenData.getUsername());
                    User user = userService.findByUsername(tokenData.getUsername());
                    if (user != null) {
                        wsSession.setUser(user);
                        //setUserForWSSession(session, user);
                    } else {
                        logger.info("User by username not found {}", tokenData.getUsername());
                        session.sendMessage(new TextMessage(errorMessage("User not found!")));
                        session.close();
                        break;
                    }
                } else {
                    session.sendMessage(new TextMessage(errorMessage("No identifiers!")));
                    session.close(CloseStatus.GOING_AWAY);
                    break;
                }
                //setTypeForWSSession(session, hello.getType());
                wsSession.setAuthorized(true);
                session.sendMessage(new TextMessage(getCmdMessage("AUTHORIZED")));
                break;

            case "DEVICECONFIG":
                // получение конфига от контроллера или фронта
                RCConfigDTO rcConfigDTO = objectMapper.readValue(json, RCConfigDTO.class);
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    rcConfigDTO.setMac(wsSession.getControllerId());
                    try {
                        // для RC перетираем весь конфиг
                        res = relayControllerService.saveNewRelayController(rcConfigDTO);
                    } catch (Exception e) {
                        logger.error(e.getLocalizedMessage());
                        res = "Wrong config format";
                    }
                    if (!"OK".equalsIgnoreCase(res)) {
                        wsSession.setAuthorized(true);
                        wsSession.sendMessage(new TextMessage(getCmdMessage("AUTHORIZED")));
                    } else {
                        wsSession.sendMessage(new TextMessage(errorMessage(res)));
                    }
                } else if ("web".equalsIgnoreCase(wsSession.getType())) {
                    // для web сохраняем только конфиг без IO
                    res = relayControllerService.saveConfig(rcConfigDTO);
                    if ("OK".equalsIgnoreCase(res))
                        wsSession.sendMessage(new TextMessage(successMessage("Saved successfully")));
                    else
                        wsSession.sendMessage(new TextMessage(errorMessage(res)));
                }
                break;

            case "DEVICECONFIGRESPONSE":
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    // результат обновления конфига от контроллера
                    Message msg = objectMapper.readValue(json, Message.class);
                    if (msg.getMessage() != null) {
                        logger.info("DEVICECONFIGRESPONSE msg {}", msg.getMessage());
                        if ("OK".equalsIgnoreCase(msg.getMessage())) {
                            sendMessageToWebUser(wsSession.getUser(), successMessage("Upload successful"));
                        } else {
                            sendMessageToWebUser(wsSession.getUser(), errorMessage("Upload to controller failed"));
                        }
                    }
                }
                break;

            case "INFO":
                // получение информации о контроллере (аптайм, и т.д.)
                if (!"web".equalsIgnoreCase(wsSession.getType())) {
                    Info info = objectMapper.readValue(json, Info.class);
                    controllerService.setControllerInfo(info);
                    // send to web
                    WSTextMessage wsMsg = new WSTextMessage("INFO", info);
                    //logger.warn(wsMsg.makeMessage());
                    sendMessageToWebUser(wsSession.getUser(), wsMsg.makeMessage());
                }
                break;

            case "IOSTATES":
                // Обновление состояния входов/выходов сразу всех. Используется при подключении контроллера
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    RCUpdateIODTO rcUpdateIODTO = objectMapper.readValue(json, RCUpdateIODTO.class);
                    rcUpdateIODTO.setMac(wsSession.getControllerId());
                    relayControllerService.updateRelayControllerIOStates(rcUpdateIODTO);
                    // send all data to web users
                    sendMessageToWebUser(wsSession.getUser(), relayControllerService.getIOStates(wsSession.getControllerId()));
                }
                break;

            case "UPDATE":
                // обновление состояния входов/выходов в базе и отправка на фронт
//            {"type":"UPDATE","payload":{"mac":"C8F09E311008","input":16,"state":"long"}}
                // проверить линковку
                if ("relayController".equalsIgnoreCase(wsSession.getType())) {
                    RCUpdateDTO update = objectMapper.readValue(json, RCUpdateDTO.class);
                    if (update.getOutput() != null) {
                        relayControllerService.setOutputState(wsSession.getControllerId(), update.getOutput(), update.getState(), update.getSlaveId());
                    } else if (update.getInput() != null) {
                        if ((update.getInput() == 16) && ("longpress".equalsIgnoreCase(update.getState()))) {
                            // event for link
                            // проверить есть ли запрос на линковку в веб сессии
                            WSSession linkSession = findSessionForLinkRequest(update.getMac());
                            if (linkSession != null) {
                                logger.info("Link request ok");
                                if (controllerService.linkController(update.getMac(), linkSession.getUser()).equalsIgnoreCase("OK")) {
                                    linkSession.getSession().sendMessage(new TextMessage(WSTextMessage.send("LINKOK", null)));
                                    wsSession.setUser(linkSession.getUser());
                                }
                            }
                        } else {
                            relayControllerService.setInputState(wsSession.getControllerId(), update.getInput(), update.getState(), update.getSlaveId());
                        }
                    }
                    // отправить уведомление владельцу контроллера
                    if (wsSession.getUser() != null) {
                        //logger.info(String.format("User found %s", user.getFio()));
                        sendMessageToWebUser(wsSession.getUser(), wsTextMessage.makeMessage());
                    } else {
                        logger.info("UPDATE. No user session found");
                    }
                }
                break;

            case "ACTION":
                // отправка действия на контроллер
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    ActionDTO actionDTO = objectMapper.readValue(json, ActionDTO.class);
                    User user = controllerService.findControllerOwner(actionDTO.getMac());
                    if (user != null && user.equals(wsSession.getUser())) {
                        logger.info("Sending message {} to controller {}", actionDTO.getAction(), actionDTO.getMac());
                        sendMessageToController(actionDTO.getMac(), wsTextMessage.makeMessage());
                    } else {
                        logger.error("Action message to {} with incorrect owner {}", actionDTO.getMac(), wsSession.getUser().getUsername());
                    }
                }
                break;

            case "UPDATEOUTPUT":
                // обновление выхода контроллера
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    RCOutputDTO rcUpdateOutputDTO = objectMapper.readValue(json, RCOutputDTO.class);
                    // обновление только в БД, конфиг потом руками на контроллер, пока только для relaycontroller
                    res = relayControllerService.updateOutput(rcUpdateOutputDTO);
                    if ("OK".equalsIgnoreCase(res))
                        wsSession.sendMessage(new TextMessage(successMessage("Saved successfully")));
                    else
                        wsSession.sendMessage(new TextMessage(errorMessage(res)));
                }
                break;

            case "UPDATEINPUT":
                // обновление входа контроллера
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    // пока только для relaycontroller
                    RCInputDTO rcUpdateInputDTO = objectMapper.readValue(json, RCInputDTO.class);
                    res = relayControllerService.updateInput(rcUpdateInputDTO);
                    if ("OK".equalsIgnoreCase(res))
                        wsSession.sendMessage(new TextMessage(successMessage("Saved successfully")));
                    else
                        wsSession.sendMessage(new TextMessage(errorMessage(res)));
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
                                wsSession.sendMessage(new TextMessage(errorMessage("Controller already linked")));
                            } else {
                                //session.sendMessage(new TextMessage(AlertMessage.makeAlert(controller.getType())));
                                // проверить онлайн ли сейчас контроллер
                                if (!isControllerOnline(linkRequest.getMac())) {
                                    wsSession.sendMessage(new TextMessage(errorMessage("Controller is offline. Make sure that is powered on and connect to internet.")));
                                } else {
                                    Map<String, Object> payld = new HashMap<>();
                                    //payld.put("message", 123);
                                    payld.put("controllertype", controller.getType());
                                    wsSession.sendMessage(new TextMessage(WSTextMessage.send("LINK", payld)));
                                    // find controller and wait link event from it
                                    // make temporary flag
                                    wsSession.setControllerId(linkRequest.getMac());
                                    wsSession.setObj("linkRequested"); // set curtime
                                }
                            }
                        } else {
                            wsSession.sendMessage(new TextMessage(errorMessage("Controller not found")));
                        }
                    } else if ("linkRequestTimeout".equalsIgnoreCase(linkRequest.getEvent())) {
                        wsSession.setObj(null);
                    } else {
                        wsSession.sendMessage(new TextMessage(errorMessage("Controller not found")));
                    }
                }
                break;

            case "COMMAND":
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    Command command = objectMapper.readValue(json, Command.class);
                    logger.info("Command {}, mac {}", command.getCommand(), command.getMac());
                    if ("enableSendLogs".equalsIgnoreCase(command.getCommand())) {
                        Map<String, Object> payld = new HashMap<>();
                        payld.put("send", true);
                        sendMessageToController(command.getMac(), WSTextMessage.send("SENDLOGS", payld));
                    } else if ("disableSendLogs".equalsIgnoreCase(command.getCommand())) {
                        Map<String, Object> payld = new HashMap<>();
                        payld.put("send", false);
                        sendMessageToController(command.getMac(), WSTextMessage.send("SENDLOGS", payld));
                    } else if ("startOTA".equalsIgnoreCase(command.getCommand())) {
                        Map<String, Object> payld = new HashMap<>();
                        payld.put("url", "https://akpeisov.kz/RelayController.bin"); // TODO : move it to env
                        sendMessageToController(command.getMac(), WSTextMessage.send("OTA", payld));
                    } else if ("INFO".equalsIgnoreCase(command.getCommand())) {
                        sendMessageToController(command.getMac(), WSTextMessage.send("INFO", null));
                    } else if ("REBOOT".equalsIgnoreCase(command.getCommand())) {
                        sendMessageToController(command.getMac(), WSTextMessage.send("REBOOT", null));
                    } else if ("UPLOADCONFIG".equalsIgnoreCase(command.getCommand())) {
                        if (isControllerOnline(command.getMac())) {
//                          // make config and send to controller
                            String deviceConfig = relayControllerService.makeDeviceConfig(command.getMac());
//                          logger.info(deviceConfig);
                            if (!"{}".equalsIgnoreCase(deviceConfig)) {
                                res = sendMessageToController(command.getMac(), deviceConfig);
                                logger.info("sendMessageToController {}", res);
                                if ("OK".equalsIgnoreCase(res)) {
                                    wsSession.sendMessage(new TextMessage(successMessage("Successfully")));
                                } else {
                                    wsSession.sendMessage(new TextMessage(errorMessage(res)));
                                }
                            } else {
                                wsSession.sendMessage(new TextMessage(errorMessage("Can't generate device config")));
                            }
                        } else {
                            wsSession.sendMessage(new TextMessage(errorMessage("Controller offline")));
                        }
                    } else if ("TEST".equalsIgnoreCase(command.getCommand())) {
                        wsSession.sendMessage(new TextMessage(successMessage("Test ok")));
                    } else if ("DELETE".equalsIgnoreCase(command.getCommand())) {
                        res = controllerService.deleteController(command.getMac());
                        if ("OK".equalsIgnoreCase(res)) {
                            wsSession.sendMessage(new TextMessage(successMessage("Controller deleted")));
                        } else {
                            wsSession.sendMessage(new TextMessage(errorMessage(res)));
                        }
                    }
                }
                break;

            case "LOG":
                if (!"web".equalsIgnoreCase(wsSession.getType())) {
                    sendMessageToWebUser(wsSession.getUser(), wsTextMessage.makeMessage());
                }
                break;

            default:
                logger.warn("Unknown message {}", type);
                logger.info(wsTextMessage.getPayload().toString());
                session.sendMessage(new TextMessage(errorMessage("Unknown message")));
                break;
        }
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
        // TODO : audit session
        WSSession wsSession = new WSSession(session);
        String clientIpAddress = (String) session.getAttributes().get(IpHandshakeInterceptor.CLIENT_IP_ADDRESS_KEY);
        if (clientIpAddress == null && session.getRemoteAddress() != null) {
            clientIpAddress = session.getRemoteAddress().toString();
        }
        wsSession.setClientIP(clientIpAddress);
        wsSessions.add(wsSession);
        logger.info("New client connected with ID {} remote IP {} client IP {}", session.getId(), session.getRemoteAddress(), clientIpAddress);
        sessionService.addSession(session.getId(), clientIpAddress);
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // find current controller and set offline
        // TODO : audit session
        sessionService.endSession(session.getId());
        String mac = getControllerIdForWSSession(session);
        if (mac != null)
            controllerService.setControllerOffline(mac);
        String type = "";
        WSSession sessionToDel = null;
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(session)) {
                type = wsSession.getType();
                sessionToDel = wsSession;
                break;
            }
        }
        if (sessionToDel != null)
            wsSessions.remove(sessionToDel);
        if ("web".equalsIgnoreCase(type)) {
            logger.info("Client {} disconnected with ID {}. Status {}", type, session.getId(), status);
        } else {
            logger.info("Client {} with mac {} disconnected", type, mac);
        }

        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.info("handleTransportError with ID {} {}", session.getId(), exception.getMessage());
    }

//    @Override
//    public boolean supportsPartialMessages() {
//        return true;
//    }

//    @Override
//    public void handlePartialMessage(WebSocketSession session, TextMessage message) throws Exception {
//        // Собираем фрагменты сообщения в буфер, сохранённый в атрибутах сессии.
//        // Когда получаем последний фрагмент, вызываем существующий handleTextMessage с объединённым сообщением.
//        Map<String, Object> attrs = session.getAttributes();
//        final String ATTR_KEY = "__partial_message_buffer";
//        StringBuilder buf = (StringBuilder) attrs.get(ATTR_KEY);
//        if (buf == null) {
//            buf = new StringBuilder();
//        }
//        buf.append(message.getPayload());
//        if (message.isLast()) {
//            // полный текст собран — удаляем буфер и передаём в обработчик полного сообщения
//            attrs.remove(ATTR_KEY);
//            TextMessage full = new TextMessage(buf.toString());
//            // Вызов handleTextMessage для дальнейшей обработки, как для обычного полного сообщения
//            handleTextMessage(session, full);
//        } else {
//            // сохраняем буфер и ждём следующих фрагментов
//            attrs.put(ATTR_KEY, buf);
//        }
//    }

    public String sendMessageToController(String controllerId, String message) {
        if (controllerId == null)
            return "MAC_NULL";

        return wsSessions.stream()
            .filter(session -> "relayController".equalsIgnoreCase(session.getType()))
            .filter(session -> controllerId.equalsIgnoreCase(session.getControllerId()))
            .findFirst()
            .map(session -> {
                try {
                    if (session.getSession().isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        return "OK";
                    } else {
                        return "SESSION_CLOSED";
                    }
                } catch (IOException e) {
                    logger.error("Error sending message to controller {}: {}", controllerId, e.getMessage());
                    return "ERROR";
                }
            })
            .orElse("NOT_FOUND");
//        for (WSSession session: wsSessions) {
//            if ("relayController".equalsIgnoreCase(session.getType()) &&
//                    controllerId.equalsIgnoreCase(session.getControllerId())) {
//                try {
//                    if (session.getSession().isOpen()) {
//                        session.sendMessage(new TextMessage(message));
//                        return "OK";
//                    }
//                } catch (IOException e) {
//                    logger.error(e.getLocalizedMessage());
//                    return "ERROR";
//                }
//                break;
//            }
//        }
//        return "NOT_FOUND";
    }

    public void sendMessageToWebUser(User user, String message) {
        // Отправка сообщения во все пользовательские сессии
        if (user == null) {
            logger.error("sendMessageToWebUser. User is null");
            return;
        }

        logger.info("sendMessageToWebUser. User {}. Message {}", user.getUsername(), message);

        for (WSSession session: wsSessions) {
            // TODO : admin/auditor role
            if ("web".equalsIgnoreCase(session.getType()) && user.equals(session.getUser())) {
                try {
                    if (session.getSession().isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        logger.info("Message to user {} sent", user.getFio());
                    }
                } catch (IOException e) {
                    logger.error(e.getLocalizedMessage());
                }
            }
        }
    }

    private WSSession findSessionForLinkRequest(String mac) {
        // определить есть ли запрос на линковку в текущих сессиях фронта
        for (WSSession wsSession : wsSessions) {
            if ("web".equals(wsSession.getType()) &&
                wsSession.getControllerId() != null &&
                wsSession.getControllerId().equalsIgnoreCase(mac) &&
                "linkRequested".equalsIgnoreCase((String)wsSession.getObj()))  {
                return wsSession;
            }
        }
        return null;
    }

    private boolean isControllerOnline(String mac) {
        if (mac == null)
            return false;
        for (WSSession wsSession : wsSessions) {
            if (mac.equalsIgnoreCase(wsSession.getControllerId())) {
                return true;
            }
        }
        return false;
    }

    /*
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

    public List<WSSession> getCurrentWSSessions() {
        return wsSessions;
    }
    */

    private String getControllerIdForWSSession(WebSocketSession targetSession) {
        for (WSSession wsSession : wsSessions) {
            if (wsSession.getSession().equals(targetSession)) {
                return wsSession.getControllerId();
            }
        }
        return null;
    }

    @Scheduled(fixedRate = 5000)
    private void alive() throws IOException {
        for (WSSession wsSession : wsSessions) {
            if (wsSession != null && "web".equalsIgnoreCase(wsSession.getType()) &&
                    wsSession.getSession().isOpen() && wsSession.isExpired()) {
                String date = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
                wsSession.sendMessage(new TextMessage(AlertMessage.makeAlert(String.format("alive %s", date))));
            }
        }
    }

    @Scheduled(fixedRate = 20000)
    private void serviceTask() {
        controllerService.setOffline();
    }
}
