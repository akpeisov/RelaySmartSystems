package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.model.Controller;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.UserDevices;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping(value = "/webapi")
//@CrossOrigin(origins = "http://localhost:8888")
public class WebAPI {
    private static final Logger logger = LoggerFactory.getLogger(WebAPI.class);
    private final RelayControllerRepository relayControllerRepository;
    private final UserService userService;
    private final ControllerService controllerService;
    private final WebSocketHandler webSocketHandler;
    private final RelayControllerService relayControllerService;
    public WebAPI(RelayControllerRepository relayControllerRepository,
                  UserService userService,
                  ControllerService controllerService,
                  WebSocketHandler webSocketHandler,
                  RelayControllerService relayControllerService) {
        this.relayControllerRepository = relayControllerRepository;
        this.userService = userService;
        this.controllerService = controllerService;
        this.webSocketHandler = webSocketHandler;
        this.relayControllerService = relayControllerService;
    }

    @GetMapping("/userDevices")
    public ResponseEntity<?> getUserDevices(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        // TODO : убрать заглушку
        if (username == null)
            username = "user";

        User user = (User) userService.findById(username).orElse(null); // orElse avoid optional cast conversion
        if (user == null) {
            // пользователь не найден. Заводим нового
            user = userService.addUser(username, (String) request.getAttribute("firstname"), (String) request.getAttribute("lastname"));
            if (user == null) {
                logger.info(String.format("Can't create new user", username));
                return ResponseEntity.status(400).body("Can't create new user");
            }
        }
        // отдаем массив устройств
        UserDevices userDevices = new UserDevices();
        userDevices.setUsername(username);
        userDevices.setUserfio(user.getFio());
        List<RelayController> relayControllers = relayControllerRepository.findByUser(user);
        userDevices.setRelayControllers(relayControllers);
        // TODO : add other controllers types (uni...)

        return ResponseEntity.ok().body(userDevices);
    }

    @PostMapping("/linkUserDevice")
    public ResponseEntity<?> linkUserDevice(HttpServletRequest request,
                                            @RequestBody String mac) {
        // проверить наличие ожидающего линковски устройства
        // если оно есть то проверить пользователя и если его нет то завести
        // вытащить ФИО, имя пользователя из токена
        // если устройства нет то вернуть 404
        // привязка устройства пользователя
        // запросить конфигу устройства
        if (mac == null) {
            return ResponseEntity.status(400).body("Mac is null");
        }
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(404).body("Username is null");
        }

        Controller controller = controllerService.findController(mac);
        if (controller.getUser() != null) {
            // уже есть какой-то юзер, что делаем?
            // если это этот же юзер или другой???
            if (controller.getUser().getId().equals(username)) {
                return ResponseEntity.status(400).body("Already linked");
            } else {
                return ResponseEntity.status(400).body("Already linked to another user");
            }
        } else {
            // ищем юзера
            User user = (User) userService.findById(username).orElse(null); // orElse avoid optional cast conversion
            if (user != null) {
                String res = controllerService.linkController(mac, user);
                if ("OK".equals(res)) {
                    webSocketHandler.requestControllerConfig(mac);
                    return ResponseEntity.ok().body("Device linked");
                }
            }
            return ResponseEntity.status(400).body("Can't link device");
        }
    }

    @PostMapping("/unlinkUserDevice")
    public ResponseEntity<?> unlinkUserDevice(HttpServletRequest request,
                                            @RequestBody String mac) {
        // проверить наличие ожидающего линковски устройства
        // если оно есть то проверить пользователя и если его нет то завести
        // вытащить ФИО, имя пользователя из токена
        // если устройства нет то вернуть 404
        // привязка устройства пользователя
        // запросить конфигу устройства
        if (mac == null) {
            return ResponseEntity.status(400).body("Mac is null");
        }
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(404).body("Username is null");
        }

        Controller controller = controllerService.findController(mac);
        if (controller.getUser() != null) {
            // уже есть какой-то юзер, что делаем?
            // если это этот же юзер или другой???
            if (controller.getUser().getId().equals(username)) {
                controllerService.unlinkController(mac, controller.getUser());
                return ResponseEntity.ok().body("Device unlinked");
            } else {
                return ResponseEntity.status(400).body("Controller linked to another user");
            }
        }
        return ResponseEntity.status(404).body("Controller not found");
    }

    @PostMapping("/requestDeviceConfig")
    public ResponseEntity<?> requestDeviceConfig(HttpServletRequest request,
                                                 @RequestBody String mac) {
        webSocketHandler.requestControllerConfig(mac);
        return ResponseEntity.ok().body("Ok");
    }

    @PostMapping(path = "/setDeviceConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setDeviceConfig(HttpServletRequest request,
                                             @RequestBody String mac) {
        if (mac == null) {
            return ResponseEntity.status(400).body("No mac");
        }
        String json = relayControllerService.makeDeviceConfig(mac);
        if (json == null) {
            return ResponseEntity.status(404).body("Config not found");
        }
        String res = webSocketHandler.SendMessageToController(mac, json);
        if (!"OK".equals(res)) {
            return ResponseEntity.status(400).body(String.format("Error while sending message %s", res));
        }
        return ResponseEntity.ok().body("Config sent");
    }

}
