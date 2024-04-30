package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RCInputRepository;
import kz.home.RelaySmartSystems.repository.RCOutputRepository;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

@Service
public class RelayControllerService {
    private final RelayControllerRepository relayControllerRepository;
    private final RCOutputRepository outputRepository;
    private final RCInputRepository inputRepository;

    public RelayControllerService(RelayControllerRepository relayControllerRepository,
                                  RCOutputRepository outputRepository,
                                  RCInputRepository inputRepository) {
        this.relayControllerRepository = relayControllerRepository;
        this.outputRepository = outputRepository;
        this.inputRepository = inputRepository;
    }

    public RelayController addRelayController(RelayController relayController, User user) {
        RelayController newRelayController = new RelayController();
        try {
            // найти существующий контроллер
            String mac = relayController.getMac();
            RelayController existingRelayController = relayControllerRepository.findByMac(mac);
            if (existingRelayController != null) {
                // нашли существующий. удаляем к чертям со всеми дочерними объектами
                relayControllerRepository.delete(existingRelayController);
            }

            // Создаем новый
            newRelayController.setMac(relayController.getMac());
//            newRelayController.setName(relayController.getName());
            newRelayController.setUser(user);

            // outputs
            List<RCOutput> newOutputs = new ArrayList<>();
            for (RCOutput output : relayController.getOutputs()) {
                RCOutput newOutput = new RCOutput();
                BeanUtils.copyProperties(newOutput, output);
                newOutput.setRelayController(newRelayController);
                newOutputs.add(newOutput);
            }
            newRelayController.setOutputs(newOutputs);

            // inputs
            List<RCInput> newInputs = new ArrayList<>();
            for (RCInput input : relayController.getInputs()) {
                RCInput newInput = new RCInput();
                BeanUtils.copyProperties(newInput, input);
                newInput.setRelayController(newRelayController);
                // rules
                List<RCRule> newRules = new ArrayList<>();
                for (RCRule rule : input.getRules()) {
                    RCRule newRule = new RCRule();
                    BeanUtils.copyProperties(newRule, rule);
                    newRule.setInput(newInput);
                    // actions (for chain rule)
                    if (rule.getActions() != null) {
                        List<RCAction> newActions = new ArrayList<>();
                        for (RCAction action : rule.getActions()) {
                            RCAction newAction = new RCAction();
                            BeanUtils.copyProperties(newAction, action);
                            newAction.setRule(newRule);
                            newActions.add(newAction);
                        }
                        newRule.setActions(newActions);
                    }
                    // -- actions
                    // acls
                    if (rule.getAcls() != null) {
                        List<RCAcl> newAcls = new ArrayList<>();
                        for (RCAcl acl : rule.getAcls()) {
                            RCAcl newAcl = new RCAcl();
                            BeanUtils.copyProperties(newAcl, acl);
                            newAcl.setRule(newRule);
                            newAcls.add(newAcl);
                        }
                        newRule.setAcls(newAcls);
                    }
                    // -- acls
                    newRules.add(newRule);
                }
                newInput.setRules(newRules);
                // -- rules
                newInputs.add(newInput);
            }
            newRelayController.setInputs(newInputs);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return relayControllerRepository.save(newRelayController);
    }

    public void setOutputState(String mac, Integer output, String state) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCOutput o = outputRepository.findOutput(c.getUuid(), output);
            if (o != null) {
                o.setState(state);
                outputRepository.save(o);
            }
        }
    }
    public void setInputState(String mac, Integer input, String state) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCInput o = inputRepository.findInput(c.getUuid(), input);
            if (o != null) {
                o.setState(state);
                inputRepository.save(o);
            }
        }
    }

    public String makeDeviceConfig(String mac) {
        // формирование конфигурации устройства для отправки на устройство
        String json = null;
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            for (RCOutput rcOutput : c.getOutputs()) {
                rcOutput.setUuid(null);
            }
            c.setMac(null);
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put("type", "SETDEVICECONFIG");
            objectMap.put("payload", c);

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                json = objectMapper.writeValueAsString(objectMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return json;
        // outputs
    }

    public User getUser(String mac) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            return c.getUser();
        }
        return null;
    }

}
