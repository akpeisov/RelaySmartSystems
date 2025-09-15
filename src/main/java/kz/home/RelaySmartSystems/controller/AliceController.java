package kz.home.RelaySmartSystems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.alice.AliceResponseError;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.alice.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.RCOutput;
import kz.home.RelaySmartSystems.repository.RCOutputRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import kz.home.RelaySmartSystems.service.AliceRequestLogService;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping(value = "alice/v1.0")
public class AliceController {
    private static final Logger logger = LoggerFactory.getLogger(AliceController.class);
    private final UserRepository userRepository;
    private final AliceRequestLogService aliceRequestLogService;
    private final RCOutputRepository rcOutputRepository;
    private final WebSocketHandler webSocketHandler;
    private final RelayControllerService relayControllerService;

    public AliceController(UserRepository userRepository,
                           AliceRequestLogService aliceRequestLogService,
                           RCOutputRepository rcOutputRepository,
                           WebSocketHandler webSocketHandler,
                           RelayControllerService relayControllerService) {
        this.userRepository = userRepository;
        this.aliceRequestLogService = aliceRequestLogService;
        this.rcOutputRepository = rcOutputRepository;
        this.webSocketHandler = webSocketHandler;
        this.relayControllerService = relayControllerService;
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null)
            ip = request.getRemoteAddr();
        return ip;
    }

    @GetMapping("")
    public void head(HttpServletRequest request) {
        // Проверка доступности Endpoint URL провайдера
        aliceRequestLogService.writeLog("head", getIp(request), "", "", "", "");
    }

    @PostMapping(path = "/user/unlink", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> unlink(HttpServletRequest request, @RequestHeader("X-Request-Id") String requestId) {
        aliceRequestLogService.writeLog("unlink", getIp(request), requestId,
                (String)request.getAttribute("username"), (String)request.getAttribute("token"), "");
        // POST	/v1.0/user/unlink	Оповещение о разъединении аккаунтов
        //-H 'Authorization: Bearer 123qwe456a...' \
        //-H 'X-Request-Id: ff36a3cc-ec...'
        // TODO : implement method
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("request_id", requestId);
        return ResponseEntity.ok().body(map);
    }

    @GetMapping(path = "/user/devices", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> getDevices(HttpServletRequest request,
                                        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        // GET	/v1.0/user/devices	Информация об устройствах пользователя
        String username = (String) request.getAttribute("username");
        logger.info("user devices. Username {}", username);
        UUID logId = aliceRequestLogService.writeLog("devices", getIp(request), requestId,
                username, (String)request.getAttribute("token"),"");
        User user = userRepository.findByUsername(username);
        if (user == null) {
            String msg = String.format("User %s not found", username);
            aliceRequestLogService.setResponse(logId, msg);
            return ResponseEntity.status(404).body(new AliceResponseError(requestId, msg));
        }

        // считаем все выходы контроллеров с признаком alice
        AliceDeviceResponse response = new AliceDeviceResponse();
        response.setRequestId(requestId);
        AlicePayload payload = new AlicePayload();
        payload.setUserId(user.getUsername());

        List<AliceDevice> aliceDevices = new ArrayList<>();
        List<RCOutput> outputs = rcOutputRepository.getAliceOutputs(user);
        for (RCOutput rcOutput: outputs) {
            aliceDevices.add(getAliceDevice(rcOutput));
        }
        payload.setDevices(aliceDevices);
        response.setPayload(payload);

        aliceRequestLogService.setResponse(logId, toJson(response));
        return ResponseEntity.ok().body(response);
    }

    private static AliceDevice getAliceDevice(RCOutput rcOutput) {
        AliceDevice aliceDevice = new AliceDevice();
        aliceDevice.setId(rcOutput.getUuid().toString());
        aliceDevice.setName(rcOutput.getName());
        aliceDevice.setDescription(rcOutput.getName());
        aliceDevice.setRoom(rcOutput.getRoom());
        aliceDevice.setType("devices.types.light"); // считаем все лампами

        AliceCapability aliceCapability = new AliceCapability();
        aliceCapability.setType("devices.capabilities.on_off");
        List<AliceCapability> aliceCapabilities = new ArrayList<>();
        aliceCapabilities.add(aliceCapability);
        aliceDevice.setAliceCapabilities(aliceCapabilities);
        return aliceDevice;
    }

    @PostMapping(path = "/user/devices/query", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> queryDevices(HttpServletRequest request,
                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                          @RequestBody AliceDeviceStateRequest deviceStateRequest) {
        String username = (String) request.getAttribute("username");
        logger.info("user devices query. Username {}", username);
        UUID logId = aliceRequestLogService.writeLog("devices/query",
                getIp(request), requestId,
                username, (String)request.getAttribute("token"), deviceStateRequest.toString());
        if (username == null) {
            String msg = "Username is null";
            aliceRequestLogService.setResponse(logId, msg);
            return ResponseEntity.status(404).body(new AliceResponseError(requestId, msg));
        }
        User user = userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
        if (user == null) {
            logger.info(String.format("User with username %s not found", username));
            aliceRequestLogService.setResponse(logId, String.format("User with username %s not found", username));
            return ResponseEntity.status(404).body(new AliceResponseError(requestId, "User not found"));
        }

        AliceDeviceStateResponse deviceStateResponse = new AliceDeviceStateResponse();
        deviceStateResponse.setRequest_id(requestId);

        AlicePayload alicePayload = new AlicePayload();
        List<AliceDevice> devices = new ArrayList<>();

        for (AliceDevice device : deviceStateRequest.getDevices()) {
            RCOutput rcOutput = rcOutputRepository.findById(UUID.fromString(device.getId())).orElse(null);
            AliceDevice aliceDevice = new AliceDevice();
            aliceDevice.setId(device.getId());
            if (rcOutput == null) {
                aliceDevice.setErrorCode("DEVICE_NOT_FOUND");
                aliceDevice.setErrorMessage("Device not found");
            } else {
                aliceDevice.setName(rcOutput.getName());
                aliceDevice.setDescription(rcOutput.getName());
                aliceDevice.setRoom(rcOutput.getRoom());
                aliceDevice.setType("devices.types.light"); // считаем все лампами

                AliceCapability aliceCapability = new AliceCapability();
                aliceCapability.setType("devices.capabilities.on_off");
                AliceState aliceState = new AliceState();
                aliceState.setInstance("on");
                aliceState.setValue("on".equalsIgnoreCase(rcOutput.getState()));
                aliceCapability.setState(aliceState);
                List<AliceCapability> aliceCapabilities = new ArrayList<>();
                aliceCapabilities.add(aliceCapability);
                aliceDevice.setAliceCapabilities(aliceCapabilities);
            }
            devices.add(aliceDevice);
        }

        alicePayload.setDevices(devices);
        deviceStateResponse.setPayload(alicePayload);

        aliceRequestLogService.setResponse(logId, toJson(deviceStateResponse));
        return ResponseEntity.ok().body(deviceStateResponse);
    }

    @PostMapping(path = "/user/devices/action", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> deviceAction(HttpServletRequest request,
                                          @RequestHeader(value = "X-Request-Id", required = false) String requestId,
                                          @RequestBody AliceDeviceActionRequest deviceActionRequest) {
        String username = (String) request.getAttribute("username");
        logger.info("user devices action. Username {}", username);
        UUID logId = aliceRequestLogService.writeLog("devices query", request.getHeader("X-Real-IP"), requestId,
                username, (String)request.getAttribute("token"), deviceActionRequest.toString());
        if (username == null) {
            aliceRequestLogService.setResponse(logId, "Username is null");
            return ResponseEntity.status(404).body(new AliceResponseError(requestId, "User not found"));
        }
        User user = (User) userRepository.findById(username).orElse(null); // orElse avoid optional cast converion
        if (user == null) {
            logger.info("User with username {} not found", username);
            aliceRequestLogService.setResponse(logId, String.format("User with username %s not found", username));
            return ResponseEntity.status(404).body(new AliceResponseError(requestId, "User not found"));
        }

        AliceDeviceActionResponse aliceDeviceActionResponse = new AliceDeviceActionResponse();
        aliceDeviceActionResponse.setRequest_id(requestId);

        List<AliceDevice> deviceList = new ArrayList<>();

        List<AliceDevice> aliceDevices = new ArrayList<>();
        for (AliceDevice aliceDevice : deviceActionRequest.getPayload().getDevices()) {
            RCOutput output = rcOutputRepository.findById(UUID.fromString(aliceDevice.getId())).orElse(null);
            AliceActionResult actionResult = new AliceActionResult();
            List<AliceCapability> aliceCapabilities = new ArrayList<>();
            if (output == null) {
                actionResult.setStatus("ERROR");
                actionResult.setErrorCode("DEVICE_NOT_FOUND");
            } else {
                // по каждому умению нужно отработать и предоставить ответ
                for (AliceCapability aliceCapability : aliceDevice.getAliceCapabilities()) {
                    AliceState aliceState = aliceCapability.getState();
                    if ("devices.capabilities.on_off".equals(aliceCapability.getType())) {
                        String value = "off";
                        if ((Boolean) aliceCapability.getState().getValue())
                            value = "on";
                        String mac = output.getRelayController().getMac();
                        String message = relayControllerService.getDeviceActionMessage(output.getId(), value, 0);
                        String res = webSocketHandler.sendMessageToController(mac, message);
                        //String res = webSocketHandler.sendDeviceAction(mac, output.getId(), value, 0);
                        // TODO : add slaveid
                        if ("OK".equals(res)) {
                            actionResult.setStatus("DONE");
                        } else if ("NOT_FOUND".equals(res)) {
                            actionResult.setStatus("ERROR");
                            actionResult.setErrorCode("DEVICE_NOT_FOUND");
                        } else {
                            actionResult.setStatus("ERROR");
                            actionResult.setErrorCode("UNKNOWN_ERROR");
                        }
                    }
                    aliceState.setActionResult(actionResult);
                    aliceCapability.setState(aliceState);
                    aliceCapabilities.add(aliceCapability);
                }
            }
            aliceDevice.setAliceCapabilities(aliceCapabilities);
            aliceDevices.add(aliceDevice);
        }
        AlicePayload alicePayload = new AlicePayload();
        alicePayload.setDevices(aliceDevices);
        aliceDeviceActionResponse.setPayload(alicePayload);

        aliceRequestLogService.setResponse(logId, toJson(aliceDeviceActionResponse));
        return ResponseEntity.ok().body(aliceDeviceActionResponse);
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
