package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.alice.ResponseError;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.alice.*;
import kz.home.RelaySmartSystems.repository.DeviceRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import kz.home.RelaySmartSystems.service.AliceRequestLogService;
import kz.home.RelaySmartSystems.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "alice/v1.0")
public class AliceController {
    private static final Logger logger = LoggerFactory.getLogger(AliceController.class);
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final AliceRequestLogService aliceRequestLogService;

    public AliceController(UserRepository userRepository,
                           DeviceRepository deviceRepository,
                           DeviceService deviceService,
                           AliceRequestLogService aliceRequestLogService) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.aliceRequestLogService = aliceRequestLogService;
    }

    @GetMapping("")
    public void head(HttpServletRequest request) {
        // Проверка доступности Endpoint URL провайдера
        aliceRequestLogService.writeLog("head", request.getHeader("X-Real-IP"), "", "", "", "");
    }

    @PostMapping(path = "/user/unlink", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unlink(HttpServletRequest request, @RequestHeader("X-Request-Id") String requestId) {
        aliceRequestLogService.writeLog("unlink", request.getHeader("X-Real-IP"), requestId,
                (String)request.getAttribute("username"), (String)request.getAttribute("token"), "");
        // POST	/v1.0/user/unlink	Оповещение о разъединении аккаунтов
        //-H 'Authorization: Bearer 123qwe456a...' \
        //-H 'X-Request-Id: ff36a3cc-ec...'
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("request_id", requestId);
        return ResponseEntity.ok().body(map);
    }

    @GetMapping(path = "/user/devices", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> getDevices(HttpServletRequest request,
                                        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        // GET	/v1.0/user/devices	Информация об устройствах пользователя
        String username = (String) request.getAttribute("username");
        logger.info(String.format("user devices. Username %s", username));
        Long logId = aliceRequestLogService.writeLog("devices", request.getHeader("X-Real-IP"), requestId,
                username, (String)request.getAttribute("token"),"");
        if (username == null) {
            aliceRequestLogService.setResponse(logId, "Username is null");
            return ResponseEntity.status(404).body(new ResponseError(requestId, "Username is null"));
        }

        User user = (User) userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
        if (user == null) {
            logger.info(String.format("User with username %s not found", username));
            aliceRequestLogService.setResponse(logId, String.format("User with username %s not found", username));
            return ResponseEntity.status(404).body(new ResponseError(requestId, "User not found"));
        }

        // get user devices
        List<Device> userDevices = deviceRepository.findByUser(user);
        DeviceResponse response = new DeviceResponse();
        response.setRequest_id(requestId);

        DeviceResponse.Payload payload = new DeviceResponse.Payload();
        payload.setUser_id(user.getId());
        payload.setDevices(userDevices);
        response.setPayload(payload);

        aliceRequestLogService.setResponse(logId, toJson(response));
        return ResponseEntity.ok().body(response);
    }

    @PostMapping(path = "/user/devices/query", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> queryDevices(HttpServletRequest request,
                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                          @RequestBody DeviceStateRequest deviceStateRequest) {
        String username = (String) request.getAttribute("username");
        logger.info(String.format("user devices query. Username %s", username));
        Long logId = aliceRequestLogService.writeLog("devices query", request.getHeader("X-Real-IP"), requestId,
                username, (String)request.getAttribute("token"), deviceStateRequest.toString());
        if (username == null) {
            aliceRequestLogService.setResponse(logId, "Username is null");
            return ResponseEntity.status(404).body(new ResponseError(requestId, "User not found"));
        }
        User user = (User) userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
        if (user == null) {
            logger.info(String.format("User with username %s not found", username));
            aliceRequestLogService.setResponse(logId, String.format("User with username %s not found", username));
            return ResponseEntity.status(404).body(new ResponseError(requestId, "User not found"));
        }

        DeviceStateResponse deviceStateResponse = new DeviceStateResponse();
        deviceStateResponse.setRequest_id(requestId);

//        DeviceStateRequest deviceStateRequest;
//        try {
//            deviceStateRequest = (DeviceStateRequest) request.getm;
//        } catch (Exception e) {
//            return ResponseEntity.status(400).body(new ResponseError(requestId, "Incorrect request"));
//        }

        List<DeviceStateResponse.Payload.Device> deviceList = new ArrayList<>();

        for (DeviceStateRequest.DeviceInfo deviceInfo : deviceStateRequest.getDevices()) {
            DeviceStateResponse.Payload.Device device = new DeviceStateResponse.Payload.Device();
            logger.info("id " + deviceInfo.getId());
            device.setId(deviceInfo.getId());
            // find device by id
            if (!deviceRepository.existsById(deviceInfo.getId())) {
                device.setError_code("DEVICE_NOT_FOUND");
                device.setError_message("Device with id not found");
            } else {
                Device dev = deviceRepository.findById(deviceInfo.getId()).orElse(null);
                if (dev != null) {
                    logger.info("dev " + dev.getDescription());
                    List<Capability> capabilities = dev.getCapabilities();
                    device.setCapabilities(capabilities);
                }
            }
            deviceList.add(device);
        }

        DeviceStateResponse.Payload payload = new DeviceStateResponse.Payload();
        payload.setDevices(deviceList);

        deviceStateResponse.setPayload(payload);

        aliceRequestLogService.setResponse(logId, toJson(deviceStateResponse));
        return ResponseEntity.ok().body(deviceStateResponse);
    }

    @PostMapping(path = "/user/devices/action", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> deviceAction(HttpServletRequest request,
                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                          @RequestBody DeviceActionRequest deviceActionRequest) {
        String username = (String) request.getAttribute("username");
        logger.info(String.format("user devices action. Username %s", username));
        Long logId = aliceRequestLogService.writeLog("devices query", request.getHeader("X-Real-IP"), requestId,
                username, (String)request.getAttribute("token"), deviceActionRequest.toString());
        if (username == null) {
            aliceRequestLogService.setResponse(logId, "Username is null");
            return ResponseEntity.status(404).body(new ResponseError(requestId, "User not found"));
        }
        User user = (User) userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
        if (user == null) {
            logger.info(String.format("User with username %s not found", username));
            aliceRequestLogService.setResponse(logId, String.format("User with username %s not found", username));
            return ResponseEntity.status(404).body(new ResponseError(requestId, "User not found"));
        }

        DeviceActionResponse deviceActionResponse = new DeviceActionResponse();
        deviceActionResponse.setRequest_id(requestId);

        List<DeviceActionResponse.Payload.Device> deviceList = new ArrayList<>();

        for (DeviceActionRequest.Payload.DeviceInfo deviceInfo : deviceActionRequest.getPayload().getDevices()) {
//            logger.info("id " + deviceInfo.getId());

            DeviceActionResponse.Payload.Device device = new DeviceActionResponse.Payload.Device();
            device.setId(deviceInfo.getId());
            // find device by id
            if (!deviceRepository.existsById(device.getId())) {
//                logger.info("no device found");
                device.setAction_result(new DeviceActionResponse.Payload.Device.ActionResult("ERROR", "DEVICE_UNREACHABLE", "Device not found"));
            } else {
                // get device
                Device dev = deviceRepository.getById(deviceInfo.getId());
                // for each capability do action
                List<DeviceActionResponse.Payload.Device.Capability> capabilities = new ArrayList<>();
                for (DeviceActionRequest.Payload.DeviceInfo.Capability capability : deviceInfo.getCapabilities()) {
//                    logger.info(capability.getType());
                    // check for existing capability
                    //String res = deviceService.setCapabilityValue(device.getId(), capability.getType(), capability.getState().toString());
                    CustomResponse resp = deviceService.setCapabilityValue(device.getId(), capability.getType(), capability.getState().toString());
                    // publish
                    // TODO : action here
//                    if (resp.getMqttTopic() != null) {
//                        String message = "OFF";
//                        if (capability.getState().getBoolValue())
//                            message = "ON";
//                        mqttService.publish(message, resp.getMqttTopic(), 0, false);
//                    }
//                    logger.info("res " + res);
//                    String errorCode = res != "DONE" ? res.split(" ", 2)[0] : null;
//                    String errorMessage = res != "DONE" ? res.split(" ", 2)[1] : null;
                    DeviceActionResponse.Payload.Device.Capability respCapability = new DeviceActionResponse.Payload.Device.Capability();
                    respCapability.setType(capability.getType());
                    DeviceActionResponse.Payload.Device.Capability.State state = new DeviceActionResponse.Payload.Device.Capability.State();
                    state.setInstance(capability.getState().getInstance());
//                    state.setAction_result(new DeviceActionResponse.Payload.Device.ActionResult(res == "DONE" ? "DONE" : "ERROR", errorCode, errorMessage));
                    state.setAction_result(new DeviceActionResponse.Payload.Device.ActionResult(resp.getStatus(), resp.getErrorCode(), resp.getErrorMessage()));
                    respCapability.setState(state);
                    capabilities.add(respCapability);
                }
                device.setCapabilities(capabilities);
            }
            deviceList.add(device);
        }

        DeviceActionResponse.Payload payload = new DeviceActionResponse.Payload();
        payload.setDevices(deviceList);

        deviceActionResponse.setPayload(payload);
        aliceRequestLogService.setResponse(logId, toJson(deviceActionResponse));
        return ResponseEntity.ok().body(deviceActionResponse);
    }

    @RequestMapping(value="**",method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?>  getAnythingelse(HttpServletRequest request, @RequestHeader(value = "Authorization", required = false) String auth) {
        String restOfTheUrl = new AntPathMatcher().extractPathWithinPattern(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString(),request.getRequestURI());

        logger.debug(String.format("restOfTheUrl %s. Auth %s", restOfTheUrl, auth));
        aliceRequestLogService.writeLog("unknown", request.getHeader("X-Real-IP"), "",
                "", "", restOfTheUrl + " " + auth);
        return ResponseEntity.status(404).body("Not found");
    }

    private String toJson(Object obj) {
        String json = "";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            json = objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            json = "";
        }
        return json;
    }
}
