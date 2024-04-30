package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RCInputRepository;
import kz.home.RelaySmartSystems.repository.RCOutputRepository;
import kz.home.RelaySmartSystems.repository.RCRuleRepository;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;

@Service
public class RelayControllerService {
    private final RelayControllerRepository relayControllerRepository;
    private final RCOutputRepository outputRepository;
    private final RCInputRepository inputRepository;
    private final RCRuleRepository rcRuleRepository;
    public RelayControllerService(RelayControllerRepository relayControllerRepository,
                                  RCOutputRepository outputRepository,
                                  RCInputRepository inputRepository,
                                  RCRuleRepository rcRuleRepository) {
        this.relayControllerRepository = relayControllerRepository;
        this.outputRepository = outputRepository;
        this.inputRepository = inputRepository;
        this.rcRuleRepository = rcRuleRepository;
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

    public void updateRelayController(RelayController relayController) {
        RelayController newRelayController = new RelayController();
        // найти существующий контроллер
        String mac = relayController.getMac();
        RelayController existingRelayController = relayControllerRepository.findByMac(mac);
        if (existingRelayController == null) {
            // контролер не найден
            return;
        }

        // outputs
        for (RCOutput output : relayController.getOutputs()) {
            RCOutput o = outputRepository.findOutput(existingRelayController.getUuid(), output.getId());
            if (o != null) {
                o.setState(output.getState());
                //o.setName(output.getName());
                //o.setOff(output.getOff());
                //o.setOn(output.getOn());
                outputRepository.save(o);
            }
        }

        // inputs
        for (RCInput input : relayController.getInputs()) {
            RCInput i = inputRepository.findInput(existingRelayController.getUuid(), input.getId());
            if (i != null) {
                //i.setName(input.getName());
                //i.setType(input.getType());
                i.setState(input.getState());
                inputRepository.save(i);
            }
        }
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

    public void updateOutput(RCUpdateOutput rcUpdateOutput) {
        RelayController controller = relayControllerRepository.findByMac(rcUpdateOutput.getMac());
        if (controller != null) {
            RCOutput rcOutput = outputRepository.findOutput(controller.getUuid(), rcUpdateOutput.getId());
            if (rcUpdateOutput.getName() != null)
                rcOutput.setName(rcUpdateOutput.getName());
            if (rcUpdateOutput.getAlice() != null)
                rcOutput.setAlice(rcUpdateOutput.getAlice());
            if (rcUpdateOutput.getDuration() != null)
                rcOutput.setDuration(rcUpdateOutput.getDuration());
            if (rcUpdateOutput.getOff() != null)
                rcOutput.setOff(rcUpdateOutput.getOff());
            if (rcUpdateOutput.getOn() != null)
                rcOutput.setOn(rcUpdateOutput.getOn());
            if (rcUpdateOutput.getType() != null)
                rcOutput.setType(rcUpdateOutput.getType());
            if (rcUpdateOutput.get_default() != null)
                rcOutput.set_default(rcUpdateOutput.get_default());
            outputRepository.save(rcOutput);
        }
    }

    public void updateInput(RCUpdateInput rcUpdateInput) {
        RelayController controller = relayControllerRepository.findByMac(rcUpdateInput.getMac());
        if (controller != null) {
            RCInput rcInput = inputRepository.findInput(controller.getUuid(), rcUpdateInput.getId());
            if (rcInput != null) {
                if (rcUpdateInput.getName() != null)
                    rcInput.setName(rcUpdateInput.getName());
                if (rcUpdateInput.getType() != null)
                    rcInput.setType(rcUpdateInput.getType());
//                if (rcUpdateInput.getRules() != null)
//                    rcInput.setRules(rcUpdateInput.getRules());

                // Обработка правил
                List<RCRule> existingRules = rcInput.getRules();
                List<RCRule> rulesToUpdate = new ArrayList<>();

                // идем по новым правилам
                for (RCRule ruleRequest : rcUpdateInput.getRules()) {
                    Optional<RCRule> existingRule = existingRules.stream()
                            .filter(rule -> rule.getCompareId().equals(ruleRequest.getCompareId()))
                            .findFirst();

                    RCRule newRule = existingRule.orElseGet(RCRule::new);
                    newRule.setEvent(ruleRequest.getEvent());
                    newRule.setAction(ruleRequest.getAction());
                    newRule.setSlaveid(ruleRequest.getSlaveid());
                    newRule.setOutput(ruleRequest.getOutput());
                    newRule.setType(ruleRequest.getType());
                    newRule.setDuration(ruleRequest.getDuration());
                    newRule.setInput(rcInput);
                    rulesToUpdate.add(newRule);
                }

                // Удаление лишних правил
                List<RCRule> rulesToDelete = existingRules.stream()
                        .filter(rule -> rulesToUpdate.stream().noneMatch(r -> r.getCompareId().equals(rule.getCompareId())))
                        .collect(Collectors.toList());
                
                //rcRuleRepository.deleteAll(rulesToDelete);
                //rulesToDelete.forEach(rcRuleRepository::delete);
                rulesToDelete.forEach(rcRule -> rcRuleRepository.deleteRule(rcRule.getUuid()));
                rcInput.setRules(rulesToUpdate);
                inputRepository.save(rcInput);
            }
        }
    }
}
