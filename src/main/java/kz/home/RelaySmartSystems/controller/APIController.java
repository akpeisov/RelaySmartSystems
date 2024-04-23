package kz.home.RelaySmartSystems.controller;

import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class APIController {
    // TODO : временный класс. Для всяких тестов
    private final UserRepository userRepository;
    private final RelayControllerRepository relayControllerRepository;
    private final RelayControllerService relayControllerService;
//    private final ModelMapper modelMapper;
//    public APIController(UserRepository userRepository,
//                         DeviceRepository deviceRepository,
//                         RelayControllerRepository relayControllerRepository,
//                         ModelMapper modelMapper) {
//        this.userRepository = userRepository;
//        this.deviceRepository = deviceRepository;
//        this.relayControllerRepository = relayControllerRepository;
//        this.modelMapper = modelMapper;
//    }
public APIController(UserRepository userRepository,
                     RelayControllerRepository relayControllerRepository, RelayControllerService relayControllerService) {
    this.userRepository = userRepository;
    this.relayControllerRepository = relayControllerRepository;
    this.relayControllerService = relayControllerService;
}

    @GetMapping("/users")
    List<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/users/{id}")
    User getUser(@PathVariable String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format("user not found %s", id)));
    }
    @PostMapping("/adduser")
    public User addUser(@RequestBody User newUser) {
        return userRepository.save(newUser);
    }

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
    }

    @PostMapping("/addrelaycontroller")
    public RelayController addRelayController(@RequestBody RelayController relayController) {
        User user = userRepository.findById("user").orElse(null);
        return relayControllerService.addRelayController(relayController, user);


        //return relayControllerRepository.save(relayController);
        //return relayControllerRepository.save(mapAndSaveRelayController(relayController));
    }

//    private RelayController mapAndSaveRelayController(RelayController relayController) {
//        RelayController newRelayController = new RelayController();
//        modelMapper.map(relayController, newRelayController);
//
//        List<Output> newOutputs = new ArrayList<>();
//        for (Output output : relayController.getOutputs()) {
//            Output newOutput = modelMapper.map(output, Output.class);
//            newOutput.setRelayController(newRelayController);
//            newOutputs.add(newOutput);
//        }
//
//        newRelayController.setOutputs(newOutputs);
//        return newRelayController;
//    }
}
