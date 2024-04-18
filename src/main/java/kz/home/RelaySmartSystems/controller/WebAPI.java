package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.UserDevices;
import kz.home.RelaySmartSystems.model.alice.ResponseError;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.DeviceRepository;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UserRepository userRepository;
    public WebAPI(RelayControllerRepository relayControllerRepository, UserRepository userRepository) {
        this.relayControllerRepository = relayControllerRepository;
        this.userRepository = userRepository;
    }

//    @GetMapping("/userDevices")
//    public ResponseEntity<?> getUserDevices(HttpServletRequest request) {
//        String username = (String) request.getAttribute("username");
//        logger.info(String.format("webapi userDevices. Username %s", username));
//        if (username == null) {
//            return ResponseEntity.status(404).body("Username is null");
//        }
//        User user = (User) userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
//        if (user == null) {
//            logger.info(String.format("User with username %s not found", username));
//            return ResponseEntity.status(404).body("User not found");
//        }
////        logger.info(user.getId());
//        RelayController relayController = relayControllerRepository.findByUser(user);
//        return ResponseEntity.ok().body(relayController);
//    }

    @GetMapping("/userDevices2")
    public ResponseEntity<?> getUserDevices2(HttpServletRequest request) {
        String username = "user";

        User user = (User) userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
        if (user == null) {
            logger.info(String.format("User with username %s not found", username));
            return ResponseEntity.status(404).body("User not found");
        }

        UserDevices userDevices = new UserDevices();
        userDevices.setUsername(username);
        userDevices.setUserfio(user.getFio());
        //List<Controller> controllers = controllerRepository.findByUser(user);
        //userDevices.setControllers(controllers);
//        logger.info(user.getId());
        //RelayController relayController = relayControllerRepository.findByUser(user);
        List<RelayController> relayControllers = relayControllerRepository.findByUser(user);
        userDevices.setRelayControllers(relayControllers);

        return ResponseEntity.ok().body(userDevices);
    }

}
