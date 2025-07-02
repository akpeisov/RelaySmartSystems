package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.Controller;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.UserDevices;
import kz.home.RelaySmartSystems.model.WSSession;
import kz.home.RelaySmartSystems.model.def.CRequest;
import kz.home.RelaySmartSystems.model.def.CResponse;
import kz.home.RelaySmartSystems.model.dto.WSSessionDTO;
import kz.home.RelaySmartSystems.model.relaycontroller.RCInput;
import kz.home.RelaySmartSystems.model.relaycontroller.RCUpdateInput;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import kz.home.RelaySmartSystems.repository.UniControllerRepository;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import kz.home.RelaySmartSystems.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/webapi")
//@CrossOrigin(origins = "http://localhost")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WebAPI {
    private static final Logger logger = LoggerFactory.getLogger(WebAPI.class);
    private final RelayControllerRepository relayControllerRepository;
    private final UniControllerRepository uniControllerRepository;
    private final UserService userService;
    private final ControllerService controllerService;
    private final WebSocketHandler webSocketHandler;
    private final RelayControllerService relayControllerService;
    public WebAPI(RelayControllerRepository relayControllerRepository,
                  UniControllerRepository uniControllerRepository,
                  UserService userService,
                  ControllerService controllerService,
                  WebSocketHandler webSocketHandler,
                  RelayControllerService relayControllerService) {
        this.relayControllerRepository = relayControllerRepository;
        this.uniControllerRepository = uniControllerRepository;
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

        User user = userService.findById(username).orElse(null); // orElse avoid optional cast conversion
        if (user == null) {
            // пользователь не найден. Заводим нового
            user = userService.addUser(username, (String) request.getAttribute("firstname"), (String) request.getAttribute("lastname"));
            if (user == null) {
                logger.info(String.format("Can't create new user %s", username));
                return ResponseEntity.status(400).body("Can't create new user");
            }
        }
        // отдаем массив устройств
        List<Object> controllers = new ArrayList<>();
        List<Controller> userControllers = controllerService.getUserControllers(user);
        for (Controller controller : userControllers) {
            if ("relayController".equalsIgnoreCase(controller.getType())) {
                controllers.add(relayControllerRepository.findById(controller.getUuid()));
            } else if ("uniController".equalsIgnoreCase(controller.getType())) {
                controllers.add(uniControllerRepository.findById(controller.getUuid()));
            }
        }
//        UserDevices userDevices = new UserDevices();
//        userDevices.setUsername(username);
//        userDevices.setUserfio(user.getFio());
//        List<Controller> controllers = controllerService.getUserControllers(user);
//        for (Controller controller : controllers) {
//            if ("relayController".equalsIgnoreCase(controller.getType())) {
//                controller.setControllerData(relayControllerRepository.findByMac(controller.getMac()));
//            }
//        }
//        userDevices.setControllers(controllers);

        //        HttpHeaders headers = new HttpHeaders();
//        headers.add("Access-Control-Allow-Origin", "*");
        //return ResponseEntity.ok().headers(headers).body(userDevices);
        return ResponseEntity.ok().body(controllers);
    }

    @GetMapping("/userDevices3")
    public ResponseEntity<?> getUserDevices3(HttpServletRequest request) {
        User user = userService.findById("user").orElse(null); // orElse avoid optional cast conversion
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        List<Object> controllers = new ArrayList<>();

        List<Controller> userControllers = controllerService.getUserControllers(user);
        for (Controller controller : userControllers) {
            if ("relayController".equalsIgnoreCase(controller.getType())) {
                controllers.add(relayControllerRepository.findById(controller.getUuid()));
                //controller.setControllerData(relayControllerRepository.findByMac(controller.getMac()));
            } else if ("uniController".equalsIgnoreCase(controller.getType())) {
                controllers.add(uniControllerRepository.findById(controller.getUuid()));
            }
        }

        //return ResponseEntity.ok().body(relayControllerRepository.findAll());
        return ResponseEntity.ok().body(controllers);
    }

//    @GetMapping("/userDevice")
//    public ResponseEntity<?> getUserDevice(HttpServletRequest request, @RequestParam String uuid) {
//        // Сервис получения одного контроллера пользователя
//        String username = (String) request.getAttribute("username");
//        // TODO : убрать заглушку
//        if (username == null)
//            username = "user";
//
//        User user = (User) userService.findById(username).orElse(null); // orElse avoid optional cast conversion
//        if (user == null) {
//            return ResponseEntity.status(404).body("User not found");
//        }
//        // отдаем массив устройств
//        UserDevices userDevices = new UserDevices();
//        userDevices.setUsername(username);
//        userDevices.setUserfio(user.getFio());
//        List<Controller> controllers = controllerService.getUserControllers(user);
//        for (Controller controller : controllers) {
//            if ("relaycontroller".equals(controller.getType())) {
//                controller.setControllerData(relayControllerRepository.findByMac(controller.getMac()));
//            }
//        }
//        userDevices.setControllers(controllers);
////        List<RelayController> relayControllers = relayControllerRepository.findByUser(user);
////        userDevices.setRelayControllers(relayControllers);
//        // TODO : add other controllers types (uni...)
//
////        HttpHeaders headers = new HttpHeaders();
////        headers.add("Access-Control-Allow-Origin", "*");
//        //return ResponseEntity.ok().headers(headers).body(userDevices);
//        return ResponseEntity.ok().body(userDevices);
//    }

    @PostMapping("/linkUserDevice")
    public ResponseEntity<?> linkUserDevice(HttpServletRequest request,
                                            @RequestBody CRequest cRequest) {
        // проверить наличие ожидающего линковки устройства
        // если оно есть то проверить пользователя и если его нет то завести
        // вытащить ФИО, имя пользователя из токена
        // если устройства нет то вернуть 404
        // привязка устройства пользователя
        // запросить конфигу устройства
        String errorText = (String) request.getAttribute("errorText");
        if (errorText != null) {
            return ResponseEntity.status(400).body(new CResponse(errorText));
        }
        if (cRequest.getMac() == null) {
            return ResponseEntity.status(400).body(new CResponse("Mac is null"));
        }
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(400).body(new CResponse("Username is null"));
        }

        Controller controller = controllerService.findController(cRequest.getMac());
        if (controller == null) {
            return ResponseEntity.status(404).body(new CResponse("Device not found"));
        }
        if (controller.getUser() != null) {
            // уже есть какой-то юзер, что делаем?
            // если это этот же юзер или другой???
            if (controller.getUser().getId().equals(username)) {
                return ResponseEntity.status(400).body(new CResponse("Already linked"));
            } else {
                return ResponseEntity.status(400).body(new CResponse("Already linked to another user"));
            }
        } else {
            // ищем юзера
            User user = (User) userService.findById(username).orElse(null); // orElse avoid optional cast conversion
            if (user != null) {
                String res = controllerService.linkController(cRequest.getMac(), user);
                if ("OK".equals(res)) {
                    //webSocketHandler.requestControllerConfig(cRequest.getMac());
                    return ResponseEntity.ok().body(new CResponse("Device linked"));
                }
            }
            return ResponseEntity.status(400).body(new CResponse("Can't link device"));
        }
    }

    @PostMapping("/unlinkUserDevice")
    public ResponseEntity<?> unlinkUserDevice(HttpServletRequest request,
                                            @RequestBody CRequest cRequest) {
        // проверить наличие ожидающего линковки устройства
        // если оно есть то проверить пользователя и если его нет то завести
        // вытащить ФИО, имя пользователя из токена
        // если устройства нет то вернуть 404
        // привязка устройства пользователя
        // запросить конфиг устройства
        if (cRequest.getMac() == null) {
            return ResponseEntity.status(400).body(new CResponse("Mac is null"));
        }
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(404).body(new CResponse("Username in token is null"));
        }

        Controller controller = controllerService.findController(cRequest.getMac());
        if (controller.getUser() != null) {
            // уже есть какой-то юзер, что делаем?
            // если это этот же юзер или другой???
            if (controller.getUser().getId().equals(username)) {
                controllerService.unlinkController(cRequest.getMac(), controller.getUser());
                return ResponseEntity.ok().body(new CResponse("Device unlinked"));
            } else {
                return ResponseEntity.status(400).body(new CResponse("Controller linked to another user"));
            }
        }
        return ResponseEntity.status(404).body(new CResponse("Controller not found"));
    }

//    @PostMapping("/requestDeviceConfig")
//    public ResponseEntity<?> requestDeviceConfig(HttpServletRequest request,
//                                                 @RequestBody String mac) {
//        webSocketHandler.requestControllerConfig(mac);
//        return ResponseEntity.ok().body("Ok");
//    }

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
        String res = webSocketHandler.sendMessageToController(mac, json);
        if (!"OK".equals(res)) {
            return ResponseEntity.status(400).body(String.format("Error while sending message %s", res));
        }
        return ResponseEntity.ok().body("Config sent");
    }

    @GetMapping(path = "/getWSSessions")
    public ResponseEntity<?> getWSSessiona(HttpServletRequest request) {
        List<WSSession> wsSessions = webSocketHandler.getCurrentWSSessions();

        List<WSSessionDTO> wsSessionDTOS = new ArrayList<>();
        for (WSSession wsSession : wsSessions) {
            WSSessionDTO wsSessionDTO = new WSSessionDTO();
            wsSessionDTO.setClientIP(wsSession.getClientIP());
            wsSessionDTO.setType(wsSession.getType());
            wsSessionDTO.setControllerId(wsSession.getControllerId());
            wsSessionDTO.setConnectionDate(wsSession.getConnectionDate());
            wsSessionDTO.setUsername(wsSession.getUsername());
            wsSessionDTOS.add(wsSessionDTO);
        }

        return ResponseEntity.ok().body(wsSessionDTOS);
    }

    @PostMapping(path = "/updateRCInput", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateRCInput(@RequestBody RCUpdateInput request) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        RCUpdateInput rcUpdateInput = objectMapper.readValue(request.get, RCUpdateInput.class);
        String result = relayControllerService.updateInput(request);
        return ResponseEntity.ok().body(result);
    }
}
