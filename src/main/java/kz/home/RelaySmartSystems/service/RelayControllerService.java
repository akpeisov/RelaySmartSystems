package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.beanutils.BeanUtils;

@Service
public class RelayControllerService {
    private final RelayControllerRepository relayControllerRepository;

    public RelayControllerService(RelayControllerRepository relayControllerRepository) {
        this.relayControllerRepository = relayControllerRepository;
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
            newRelayController.setName(relayController.getName());
            newRelayController.setUser(user);

            // outputs
            List<Output> newOutputs = new ArrayList<>();
            for (Output output : relayController.getOutputs()) {
                Output newOutput = new Output();
                BeanUtils.copyProperties(newOutput, output);
                newOutput.setRelayController(newRelayController);
                newOutputs.add(newOutput);
            }
            newRelayController.setOutputs(newOutputs);

            // inputs
            List<Input> newInputs = new ArrayList<>();
            for (Input input : relayController.getInputs()) {
                Input newInput = new Input();
                BeanUtils.copyProperties(newInput, input);
                newInput.setRelayController(newRelayController);
                // rules
                List<Rule> newRules = new ArrayList<>();
                for (Rule rule : input.getRules()) {
                    Rule newRule = new Rule();
                    BeanUtils.copyProperties(newRule, rule);
                    newRule.setInput(newInput);
                    // actions (for chain rule)
                    if (rule.getActions() != null) {
                        List<Action> newActions = new ArrayList<>();
                        for (Action action : rule.getActions()) {
                            Action newAction = new Action();
                            BeanUtils.copyProperties(newAction, action);
                            newAction.setRule(newRule);
                            newActions.add(newAction);
                        }
                        newRule.setActions(newActions);
                    }
                    // -- actions
                    // acls
                    if (rule.getAcls() != null) {
                        List<Acl> newAcls = new ArrayList<>();
                        for (Acl acl : rule.getAcls()) {
                            Acl newAcl = new Acl();
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


    /*
    Добавление устройства
    * ситуация 1. В базе пусто, подключаем новое устройство.
    * ситуация 2. В базе что-то есть, подключаем новое устройство. Базу чистить при этом?
    Обновление информации.
    * ситуация 3. В базе уже ранее был конфиг, но устройство стерлось, поломалось, после ремонта. Тогда оно уже привязано и можно слить конфигу?
    * */
}
