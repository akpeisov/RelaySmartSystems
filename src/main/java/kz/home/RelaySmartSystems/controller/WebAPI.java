package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.model.Role;
import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.mapper.RCConfigMapper;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.service.ControllerService;
import kz.home.RelaySmartSystems.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/webapi")
//@CrossOrigin(origins = "http://localhost")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WebAPI {
    private static final Logger logger = LoggerFactory.getLogger(WebAPI.class);
    private final UserService userService;
    private final ControllerService controllerService;
    private final RCConfigMapper rcConfigMapper;
    @Value("${env}")
    private String env;
    public WebAPI(UserService userService,
                  ControllerService controllerService,
                  RCConfigMapper rcConfigMapper) {
        this.userService = userService;
        this.controllerService = controllerService;
        this.rcConfigMapper = rcConfigMapper;
    }

    @GetMapping("/userDevices")
    public ResponseEntity<?> getUserDevices(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        User user = userService.findByUsername(username);
        logger.debug("username {}", username);
        if ("dev".equalsIgnoreCase(env) && username == null)
            username = "admin";
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // отдаем массив устройств
        List<Controller> userControllers;
        List<Object> controllers = new ArrayList<>();
        if (Role.ROLE_ADMIN.equals(user.getRole()) || Role.ROLE_AUDITOR.equals(user.getRole())) {
            userControllers = controllerService.getAllControllers();
        } else {
            userControllers = controllerService.getUserControllers(user);
        }
        for (Controller controller : userControllers) {
            if ("relayController".equalsIgnoreCase(controller.getType()) && controller instanceof RelayController rc) {
                controllers.add(rcConfigMapper.RCtoDto(rc));
            }
        }
        return ResponseEntity.ok().body(controllers);
    }
}
