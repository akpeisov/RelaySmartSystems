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
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        //sessionService.updateLastActive(session.getId());
        logger.info("pong message {}", message.getPayload());
        super.handlePongMessage(session, message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        sessionService.updateLastActive(session.getId());
        sessionService.storeMessage(session.getId(), message.getPayload());
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
        logger.debug(payload);

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
                        logger.debug("Controller {} not found. Request config", mac);
                        session.sendMessage(new TextMessage(getCmdMessage("GETDEVICECONFIG")));
                        break;
                    } else {
                        controllerService.setControllerOnline(mac);
                        if (controllerService.isControllerLinked(mac)) {
                            wsSession.setUser(controllerService.findControllerOwner(mac));
                        }
                        sendMessageToWebUser(wsSession.getUser(), message("STATUS", controllerStatus(mac, "online")));
                    }
                } else if (tokenData.getUsername() != null) {
                    // Это клиент фронта
                    sessionService.setUsername(session.getId(), tokenData.getUsername());
                    //wsSession.setUsername(tokenData.getUsername());
                    User user = userService.findByUsername(tokenData.getUsername());
                    if (user != null) {
                        wsSession.setUser(user);
                    } else {
                        logger.error("User by username not found {}", tokenData.getUsername());
                        session.sendMessage(new TextMessage(errorMessage("User not found!")));
                        session.close(CloseStatus.GOING_AWAY);
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
                if (!"web".equalsIgnoreCase(wsSession.getType())) {
                    logger.info("Controller connected. Mac {}. Owner {}", wsSession.getControllerId(), wsSession.getUser() != null ? wsSession.getUser().getUsername() : "none");
                } else {
                    logger.info("Web client connected. Username {}", wsSession.getUser().getUsername());
                }
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
                        logger.debug("DEVICECONFIGRESPONSE msg {}", msg.getMessage());
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
                // проверить линковку {"type":"UPDATE","payload":{"mac":"C8F09E311008","input":16,"state":"long"}}
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
                                logger.info("Link request ok for mac {}", wsSession.getControllerId());
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
                        sendMessageToWebUser(wsSession.getUser(), wsTextMessage.makeMessage());
                    } else {
                        logger.debug("UPDATE. No user session found");
                    }
                }
                break;

            case "ACTION":
                // отправка действия на контроллер
                if ("web".equalsIgnoreCase(wsSession.getType())) {
                    ActionDTO actionDTO = objectMapper.readValue(json, ActionDTO.class);
                    User user = controllerService.findControllerOwner(actionDTO.getMac());
                    if ((wsSession.getUser() != null && wsSession.getUser().isAdmin()) ||
                            (user != null && user.equals(wsSession.getUser()))) {
                        logger.debug("Sending message {} to controller {}", actionDTO.getAction(), actionDTO.getMac());
                        sendMessageToController(actionDTO.getMac(), wsTextMessage.makeMessage());
                    } else {
                        logger.debug("Action message to {} with incorrect owner {}", actionDTO.getMac(), wsSession.getUser().getUsername());
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
                    logger.debug("Command {}, mac {}", command.getCommand(), command.getMac());
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
                                    wsSession.sendMessage(new TextMessage(successMessage("Successfully sent")));
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

            case "ERROR":
                // получение ошибки от контроллера
                if (!"web".equalsIgnoreCase(wsSession.getType())) {
                    Message msg = objectMapper.readValue(json, Message.class);
                    logger.error("Error from controller {}: {}", wsSession.getControllerId(), msg.getMessage());
                    sendMessageToWebUser(wsSession.getUser(), errorMessage(msg.getMessage()));
                }
                break;

            default:
                logger.error("Type {}. Unknown message {}", type, wsTextMessage.getPayload().toString());
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

    private String message(String type, Object message) {
        return WSTextMessage.send(type, message);
    }

    private Object controllerStatus(String mac, String status) {
        Map<String, Object> payld = new HashMap<>();
        payld.put("mac", mac);
        payld.put("status", status);
        return payld;
    }

    private String getCmdMessage(String cmd) {
        return WSTextMessage.send(cmd, null);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WSSession wsSession = new WSSession(session);
        String clientIpAddress = (String) session.getAttributes().get(IpHandshakeInterceptor.CLIENT_IP_ADDRESS_KEY);
        wsSession.setClientIP(clientIpAddress);
        wsSessions.add(wsSession);
        logger.debug("New client connected with ID {} client IP {}", session.getId(), clientIpAddress);
        sessionService.addSession(session.getId(), clientIpAddress);
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Throwable lastError = (Throwable) session.getAttributes().get("lastError");
        String message;
        if (lastError != null) {
            message = lastError.getMessage();
        } else {
            message = status.getReason();
        }
        sessionService.endSession(session.getId(), String.format("%d%s", status.getCode(), message == null ? "" : ", " + message));
        // find current controller and set offline
        String mac = getControllerIdForWSSession(session);
        if (mac != null) {
            controllerService.setControllerOffline(mac);
            sendMessageToWebUser(getUserForSession(session), message("STATUS", controllerStatus(mac, "offline")));
        }
        logger.info("Client {} disconnected. Code {}, reason {}", mac == null ? "web " + session.getId() : "controller " + mac, status.getCode(), status.getReason());

        wsSessions.removeIf(s -> s.getSession().equals(session));

        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("handleTransportError with ID {} {}", session.getId(), exception.getMessage());
        session.getAttributes().put("lastError", exception);
    }

    private User getUserForSession(WebSocketSession targetSession) {
        Optional<WSSession> session = wsSessions.stream()
                .filter(s -> s.getSession().equals(targetSession))
                .findFirst();
        return session.map(WSSession::getUser).orElse(null);
    }

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
    }

    public void sendMessageToWebUser(User user, String message) {
        // Отправка сообщения во все пользовательские сессии
        logger.debug("sendMessageToWebUser. User {}. Message {}", user == null ? "no_user" : user.getUsername(), message);
        for (WSSession session: wsSessions) {
            if ("web".equalsIgnoreCase(session.getType()) &&
                    (session.getUser() != null && (session.getUser().isAdmin() || session.getUser().equals(user)))) {
                try {
                    if (session.getSession().isOpen()) {
                        session.sendMessage(new TextMessage(message));
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
        return wsSessions.stream().anyMatch(session -> mac.equalsIgnoreCase(session.getControllerId()));
    }

    private String getControllerIdForWSSession(WebSocketSession targetSession) {
        Optional<WSSession> session = wsSessions.stream()
                .filter(s -> s.getSession().equals(targetSession))
                .findFirst();
        return session.map(WSSession::getControllerId).orElse(null);
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
