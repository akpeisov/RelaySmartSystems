package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.alice.Capability;
import kz.home.RelaySmartSystems.repository.CapabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CapabilityService {
    private static final Logger logger = LoggerFactory.getLogger(CapabilityService.class);
    private final CapabilityRepository capabilityRepository;

    @Autowired
    public CapabilityService(CapabilityRepository capabilityRepository) {
        this.capabilityRepository = capabilityRepository;
    }

//    public void setCapabilityValueByTopic(String topic, String state) {
//        logger.debug("setCapabilityValueByTopic. Topic {} state {}", topic, state);
//        Capability capability = capabilityRepository.findByMqttTopic(topic);
//        logger.debug("capabilityId {}", capability.getId());
//        if (capability.getId() > 0) {
//            capability.setState(state);
//            capabilityRepository.save(capability);
//        }
//    }

//    public List<String> getAllTopicsForSubscribe() {
//        return capabilityRepository.getAllTopicForSubscribe();
//    }
}
