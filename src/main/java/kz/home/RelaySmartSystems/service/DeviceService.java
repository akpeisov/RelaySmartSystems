package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.*;
import kz.home.RelaySmartSystems.model.alice.Capability;
import kz.home.RelaySmartSystems.model.alice.CustomResponse;
import kz.home.RelaySmartSystems.model.alice.Device;
import kz.home.RelaySmartSystems.repository.CapabilityRepository;
import kz.home.RelaySmartSystems.repository.DeviceRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final CapabilityRepository capabilityRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);


    @Autowired
    public DeviceService(DeviceRepository deviceRepository, CapabilityRepository capabilityRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.capabilityRepository = capabilityRepository;
        this.userRepository = userRepository;
    }

    public Device findById(String id) {
        return deviceRepository.getOne(id);
    }

    public List<Device> getUserDevices(User user) {
        return deviceRepository.findByUser(user);
    }

    public CustomResponse setCapabilityValue(String deviceId, String capabilityType, String state) {
        // find device, find capability, set new value
        CustomResponse response = new CustomResponse();
        if (!deviceRepository.existsById(deviceId)) {
            //return "DEVICE_NOT_FOUND Device not found";
            response.setStatus("ERROR");
            response.setErrorCode("DEVICE_NOT_FOUND");
            response.setErrorMessage("Device not found");
            return response;
        }
        Device device = deviceRepository.getReferenceById(deviceId);

        for (Capability capability : device.getCapabilities()) {
            if (capability.getType().equals(capabilityType)) {
                capability.setState(state);
                capabilityRepository.save(capability);
                //response.setMqttTopic(capability.getMqttTopic());
                // publish
//                if (capability.getMqttTopic() != null) {
//                    // TODO : add mapping
//                    String message = "";
//                    ObjectMapper objectMapper = new ObjectMapper();
//                    try {
//                        DeviceActionRequest.Payload.DeviceInfo.Capability.State stateObject =
//                                objectMapper.readValue(state, DeviceActionRequest.Payload.DeviceInfo.Capability.State.class);
//                        if (stateObject.getValue().equals(true)) {
//                            message = "ON";
//                        } else if (stateObject.getValue().equals(false)) {
//                            message = "OFF";
//                        }
//                    } catch (JsonProcessingException e) {
//                        //throw new RuntimeException(e);
//                    }
//
//                    mqttService.publish(message, capability.getMqttTopic(), 0, false);
//                }
                response.setStatus("DONE");
                return response;
            }
        }
        response.setStatus("ERROR");
        response.setErrorCode("NO_CAPABILITY");
        response.setErrorMessage("No capability found");
        return response; //"NO_CAPABILITY No capability found";
    }

    public void updateDevice(Device newDevice, String userId) {
        // update device data in database
        //if (deviceRepository.existsById(device.getId())
//        for (Capability capability : newDevice.getCapabilities()) {
//            logger.info("cap " + capability.getType() + " " +
//                    capability.getMqttTopic() + " " +
//                    capability.getMqttStateTopic());
//        }
        logger.debug("process device " + newDevice.getId());

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.error("User not found! " + userId);
            return;
        }

        Device device = deviceRepository.findById(newDevice.getId()).orElse(null);
        if (device == null) { //.isEmpty()
            // new device
            logger.debug("New device");
            newDevice.setUser(user);
            newDevice.getCapabilities().forEach((capability -> {
                capability.setDevice(newDevice);
                //capabilityRepository.save(capability);
            }));
            deviceRepository.save(newDevice);
        } else {
            // existing device
            logger.debug("Existing device");
            // get capabilities list of new device
            for (Capability capabilityNew : newDevice.getCapabilities()) {
                boolean found = false;
                for (Capability capability : device.getCapabilities()) {
                    if (capabilityNew.getType().equals(capability.getType())) {
//                        if (!Objects.equals(capabilityNew.getMqttTopic(), capability.getMqttTopic())) {
//                            capability.setMqttTopic(capabilityNew.getMqttTopic());
//                        }
//                        if (!Objects.equals(capabilityNew.getMqttStateTopic(), capability.getMqttStateTopic())) {
//                            capability.setMqttStateTopic(capabilityNew.getMqttStateTopic());
//                        }
                        if (!Objects.equals(capabilityNew.getParameters(), capability.getParameters())) {
                            capability.setParameters(capabilityNew.getParameters());
                        }
                        //capability.setDevice(device);
                        logger.debug("Saving capability " + capability.getType());
                        capabilityRepository.save(capability);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // save as new capability
                    logger.debug("not found. save as new capability");
                    capabilityRepository.save(capabilityNew);
                }
            }
            // remove old capabilities
            // не получается реализовать, не удалается запись отдельно
//            for (Capability capability : device.getCapabilities()) {
//                boolean found = false;
//                for (Capability capabilityNew : newDevice.getCapabilities()) {
//                    if (Objects.equals(capabilityNew.getType(), capability.getType())) {
//                        found = true;
//                    }
//                }
//                if (!found) {
//                    logger.info("removing capability " + capability.getId());
//                    capabilityRepository.delete(capability);
//                    //capabilityRepository.deleteCapability(capability.getId());
//                }
//            }
            if (!Objects.equals(device.getName(), newDevice.getName())) {
                device.setName(newDevice.getName());
            }
            if (!Objects.equals(device.getDescription(), newDevice.getDescription())) {
                device.setDescription(newDevice.getDescription());
            }
            if (!Objects.equals(device.getRoom(), newDevice.getRoom())) {
                device.setRoom(newDevice.getRoom());
            }
            deviceRepository.save(device);
//            logger.info("saving device");
//            deviceRepository.save(newDevice);
        }
        //logger.info("is empty " + device.isEmpty() + " " + newDevice.getId());
    }
}