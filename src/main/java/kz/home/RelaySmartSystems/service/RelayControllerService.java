package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;

import kz.home.RelaySmartSystems.Utils;

@Service
public class RelayControllerService {
    private final RelayControllerRepository relayControllerRepository;
    private final RCOutputRepository outputRepository;
    private final RCInputRepository inputRepository;
    private final RCEventRepository rcEventRepository;
    private final RCActionRepository rcActionRepository;
    private final RCAclRepository rcAclRepository;
    private static final Logger logger = LoggerFactory.getLogger(RelayControllerService.class);
    public RelayControllerService(RelayControllerRepository relayControllerRepository,
                                  RCOutputRepository outputRepository,
                                  RCInputRepository inputRepository,
                                  RCEventRepository rcEventRepository,
                                  RCActionRepository rcActionRepository,
                                  RCAclRepository rcAclRepository) {
        this.relayControllerRepository = relayControllerRepository;
        this.outputRepository = outputRepository;
        this.inputRepository = inputRepository;
        this.rcEventRepository = rcEventRepository;
        this.rcActionRepository = rcActionRepository;
        this.rcAclRepository = rcAclRepository;
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
            newRelayController.setType("relayController");
            newRelayController.setStatus("online");
            newRelayController.setConfigTime(Instant.now().toEpochMilli() / 1000);

            // outputs
            Set<RCOutput> newOutputs = new HashSet<>();
            for (RCOutput output : relayController.getOutputs()) {
                RCOutput newOutput = new RCOutput();
                BeanUtils.copyProperties(newOutput, output);
                newOutput.setRelayController(newRelayController);
                newOutputs.add(newOutput);
            }
            newRelayController.setOutputs(newOutputs);

            // inputs
            Set<RCInput> newInputs = new HashSet<>();
            for (RCInput input : relayController.getInputs()) {
                RCInput newInput = new RCInput();
                BeanUtils.copyProperties(newInput, input);
                newInput.setRelayController(newRelayController);
                // events
                if (input.getEvents() != null) {
                    Set<RCEvent> newEvents = new HashSet<>();
                    for (RCEvent event : input.getEvents()) {
                        RCEvent newEvent = new RCEvent();
                        BeanUtils.copyProperties(newEvent, event);
                        newEvent.setInput(newInput);
                        if (event.getActions() != null) {
                            Set<RCAction> newActions = new HashSet<>();
                            for (RCAction action : event.getActions()) {
                                RCAction newAction = new RCAction();
                                BeanUtils.copyProperties(newAction, action);
                                newAction.setEvent(newEvent);
                                newActions.add(newAction);
                            }
                            newEvent.setActions(newActions);
                        }
                        // acls
                        if (event.getAcls() != null) {
                            Set<RCAcl> newAcls = new HashSet<>();
                            for (RCAcl acl : event.getAcls()) {
                                RCAcl newAcl = new RCAcl();
                                BeanUtils.copyProperties(newAcl, acl);
                                newAcl.setEvent(newEvent);
                                newAcls.add(newAcl);
                            }
                            newEvent.setAcls(newAcls);
                        }
                        newEvents.add(newEvent);
                    }
                    newInput.setEvents(newEvents);
                }
                newInputs.add(newInput);
            }
            newRelayController.setInputs(newInputs);

        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return relayControllerRepository.save(newRelayController);
    }

    public void updateRelayControllerIOStates(RelayController relayController) {
//        RelayController newRelayController = new RelayController();
        // найти существующий контроллер
        String mac = relayController.getMac();
        RelayController existingRelayController = relayControllerRepository.findByMac(mac);
        if (existingRelayController == null) {
            // контролер не найден
            logger.error(String.format("Relay controller with mac %s not found", mac));
            return;
        }

        // outputs
        for (RCOutput output : relayController.getOutputs()) {
            RCOutput o = outputRepository.findOutput(existingRelayController.getUuid(), output.getId(), output.getSlaveId());
            if (o != null) {
                o.setState(output.getState());
                outputRepository.save(o);
            }
        }

        // inputs
        for (RCInput input : relayController.getInputs()) {
            RCInput i = inputRepository.findInput(existingRelayController.getUuid(), input.getId());
            if (i != null) {
                i.setState(input.getState());
                inputRepository.save(i);
            }
        }
    }

    public void setOutputState(String mac, Integer output, String state, Integer slaveId) {
        //logger.info(String.format("setOutputState mac %s output %d state %s slaveid %d", mac, output, state, slaveId));
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCOutput o = outputRepository.findOutput(c.getUuid(), output, slaveId);
            if (o != null) {
                o.setState(state);
            } else {
                // create new output
                o = new RCOutput();
                o.setName(String.format("Out %d", output));
                o.setState(state);
                o.setRelayController(c);
                o.setSlaveId(slaveId);
                o.setId(output);
                o.setLimit(0L);
                o.setTimer(0L);
            }
            outputRepository.save(o);
        } else {
            logger.error(String.format("setOutputState. RelayController with mac %s not found", mac));
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
        String json = "{}";
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
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

        json = Utils.removeFieldsJSON(json, "uuid", "alice", "timer", "outputID", "mac", "firstDate", "linkDate", "status", "uptime", "freeMemory", "version", "linked", "lastSeen", "wifirssi");
        return json;
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
            RCOutput rcOutput = outputRepository.findOutput(controller.getUuid(), rcUpdateOutput.getId(), rcUpdateOutput.getSlaveId());
            if (rcUpdateOutput.getName() != null)
                rcOutput.setName(rcUpdateOutput.getName());
            if (rcUpdateOutput.getAlice() != null)
                rcOutput.setAlice(rcUpdateOutput.getAlice());
            if (rcUpdateOutput.getTimer() != null)
                rcOutput.setTimer(rcUpdateOutput.getTimer());
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

    public String updateInput(RCUpdateInput rcUpdateInput) {
        String result = "OK";

        Optional<RCInput> rcInputOpt = inputRepository.findById(rcUpdateInput.getUuid());
        try {
            if (rcInputOpt.isPresent()) {
                RCInput rcInput = rcInputOpt.get();
                if (rcUpdateInput.getName() != null)
                    rcInput.setName(rcUpdateInput.getName());
                if (rcUpdateInput.getType() != null)
                    rcInput.setType(rcUpdateInput.getType());

                // Обработка событий
                Set<RCEvent> existingEvents = rcInput.getEvents();
                Set<RCEvent> eventsToUpdate = new HashSet<>();

                // идем по событиям из запроса
                for (RCEvent eventRequest : rcUpdateInput.getEvents()) {
                    Optional<RCEvent> existingEvent = existingEvents.stream()
                            .filter(event -> event.getEvent().equals(eventRequest.getEvent()))
                            .findFirst();

                    RCEvent newEvent = existingEvent.orElseGet(RCEvent::new);
                    newEvent.setInput(rcInput);
                    if (existingEvent.isEmpty()) {
                        // новое событие (ранее не было), значит ВСЕ дочерние элементы нужно просто добавить
                        newEvent.setEvent(eventRequest.getEvent());
                        Set<RCAction> actionsToInsert = new HashSet<>();
                        for (RCAction actionRequest : eventRequest.getActions()) {
                            // fix
                            if (((actionRequest.getOutput() == null) || (actionRequest.getSlaveId() == null)) &&
                                (actionRequest.getOutputID() != null)) {
                                String outputID = actionRequest.getOutputID();
                                Pattern pattern = Pattern.compile("s(\\d+)o(\\d+)");
                                Matcher matcher = pattern.matcher(outputID);
                                if (matcher.matches()) {
                                    actionRequest.setSlaveId(Integer.parseInt(matcher.group(1)));
                                    actionRequest.setOutput(Integer.parseInt(matcher.group(2)));
                                }
                            }
                            RCAction newAction = new RCAction();
                            newAction.setAction(actionRequest.getAction());
                            newAction.setSlaveId(actionRequest.getSlaveId());
                            newAction.setOutput(actionRequest.getOutput());
                            newAction.setDuration(actionRequest.getDuration());
                            newAction.setOrder(actionRequest.getOrder());
                            newAction.setEvent(newEvent);
                            actionsToInsert.add(newAction);
                        }
                        if (eventRequest.getAcls() != null) {
                            Set<RCAcl> aclsToInsert = new HashSet<>();
                            for (RCAcl aclRequest : eventRequest.getAcls()) {
                                RCAcl newAcl = new RCAcl();
                                newAcl.setId(aclRequest.getId());
                                newAcl.setIo(aclRequest.getIo());
                                newAcl.setState(aclRequest.getState());
                                newAcl.setType(aclRequest.getType());
                                newAcl.setEvent(newEvent);
                                aclsToInsert.add(newAcl);
                            }
                            newEvent.setAcls(aclsToInsert);
                        }
                        newEvent.setActions(actionsToInsert);


                    } else {
                        // существующее событие, придется сравнивать экшены и асл
                        // сравниваем экшены. Сначала добавляем новые экшены
                        Set<RCAction> existingActions = existingEvent.get().getActions();
                        Set<RCAction> actionsToUpdate = new HashSet<>();
                        for (RCAction actionRequest : eventRequest.getActions()) {
                            Optional<RCAction> existingAction = existingActions.stream()
                                    .filter(action -> action.getOrder().equals(actionRequest.getOrder()))
                                    .findFirst();
                            // Может быть найдено действие, а может и нет
                            // если нет то просто добавляем, если есть обновляем
                            if (((actionRequest.getOutput() == null) || (actionRequest.getSlaveId() == null)) &&
                                    (actionRequest.getOutputID() != null)) {
                                String outputID = actionRequest.getOutputID();
                                Pattern pattern = Pattern.compile("s(\\d+)o(\\d+)");
                                Matcher matcher = pattern.matcher(outputID);
                                if (matcher.matches()) {
                                    actionRequest.setSlaveId(Integer.parseInt(matcher.group(1)));
                                    actionRequest.setOutput(Integer.parseInt(matcher.group(2)));
                                }
                            }
                            RCAction newAction = existingAction.orElseGet(RCAction::new);
                            newAction.setAction(actionRequest.getAction());
                            newAction.setSlaveId(actionRequest.getSlaveId());
                            newAction.setOutput(actionRequest.getOutput());
                            newAction.setOrder(actionRequest.getOrder());
                            newAction.setDuration(actionRequest.getDuration());
                            newAction.setEvent(newEvent);
                            actionsToUpdate.add(newAction);
                        }
                        // удаление неиспользуемых экшенов
                        List<RCAction> actionsToDelete = existingActions.stream()
                                .filter(action -> actionsToUpdate.stream()
                                        .noneMatch(r -> r.getOrder().equals(action.getOrder())))
                                .collect(Collectors.toList());
                        actionsToDelete.forEach(rcAction -> rcActionRepository.deleteRCAction(rcAction.getUuid()));

                        newEvent.setActions(actionsToUpdate);
                        // ACLs
                        Set<RCAcl> existingAcls = existingEvent.map(RCEvent::getAcls).orElseGet(HashSet::new);
                        Set<RCAcl> aclsToUpdate = new HashSet<>();
                        Set<RCAcl> requestAcls = Optional.ofNullable(eventRequest.getAcls())
                                .orElseGet(HashSet::new);
                        for (RCAcl aclRequest : requestAcls) {
                            Optional<RCAcl> existingAcl = existingAcls.stream()
                                    .filter(acl -> acl.getCompareId().equals(aclRequest.getCompareId()))
                                    .findFirst();
                            // Может быть найдено действие, а может и нет
                            // если нет то просто добавляем, если есть обновляем
                            RCAcl newAcl = existingAcl.orElseGet(RCAcl::new);
                            newAcl.setEvent(newEvent);
                            newAcl.setState(aclRequest.getState());
                            newAcl.setIo(aclRequest.getIo());
                            newAcl.setId(aclRequest.getId());
                            newAcl.setType(aclRequest.getType());
                            aclsToUpdate.add(newAcl);
                        }
                        // Удаление неиспользуемых ACL
                        Set<RCAcl> aclsToDelete = existingAcls.stream()
                                .filter(acl -> aclsToUpdate.stream()
                                        .noneMatch(r -> r.getCompareId().equals(acl.getCompareId())))
                                .collect(Collectors.toSet());
                        aclsToDelete.forEach(rcAcl -> rcAclRepository.deleteRCAcl(rcAcl.getUuid()));

                        newEvent.setAcls(aclsToUpdate);
                    }
                    eventsToUpdate.add(newEvent);
                }

                // Удаление лишних событий у входа
                List<RCEvent> eventsToDelete = existingEvents.stream()
                        .filter(event -> eventsToUpdate.stream()
                                .noneMatch(r -> r.getEvent().equals(event.getEvent())))
                        .collect(Collectors.toList());

                rcEventRepository.deleteAll(eventsToDelete);
                eventsToDelete.forEach(rcEvent -> rcEventRepository.deleteEvent(rcEvent.getUuid()));
                rcInput.setEvents(eventsToUpdate);
                inputRepository.save(rcInput);
            }
        } catch (Exception e) {
            logger.error("Error while update input", e);
            result = e.getLocalizedMessage();
        }
        return result;
    }


    /*
    * public boolean updateInput(RCUpdateInput rcUpdateInput) {
        boolean result = false;
        RelayController controller = relayControllerRepository.findByMac(rcUpdateInput.getMac());
        if (controller != null) {
            //RCInput rcInput = inputRepository.findInput(controller.getUuid(), rcUpdateInput.getId());
            RCInput rcInput = inputRepository.getReferenceById(rcUpdateInput.getUuid());
            try {
                if (rcInput != null) {
                    if (rcUpdateInput.getName() != null)
                        rcInput.setName(rcUpdateInput.getName());
                    if (rcUpdateInput.getType() != null)
                        rcInput.setType(rcUpdateInput.getType());

                    // Обработка событий
                    List<RCEvent> existingEvents = rcInput.getEvents();
                    List<RCEvent> eventsToUpdate = new ArrayList<>();

                    // идем по событиям из запроса
                    for (RCEvent eventRequest : rcUpdateInput.getEvents()) {
                        Optional<RCEvent> existingEvent = existingEvents.stream()
                                .filter(event -> event.getEvent().equals(eventRequest.getEvent()))
                                .findFirst();

                        RCEvent newEvent = existingEvent.orElseGet(RCEvent::new);
                        newEvent.setInput(rcInput);
                        if (existingEvent.isEmpty()) {
                            // новое событие (ранее не было), значит ВСЕ дочерние элементы нужно просто добавить
                            newEvent.setEvent(eventRequest.getEvent());
                            List<RCAction> actionsToInsert = new ArrayList<>();
                            for (RCAction actionRequest : eventRequest.getActions()) {
                                // fix
                                if (((actionRequest.getOutput() == null) || (actionRequest.getSlaveId() == null)) &&
                                    (actionRequest.getOutputID() != null)) {
                                    String outputID = actionRequest.getOutputID();
                                    Pattern pattern = Pattern.compile("s(\\d+)o(\\d+)");
                                    Matcher matcher = pattern.matcher(outputID);
                                    if (matcher.matches()) {
                                        actionRequest.setSlaveId(Integer.parseInt(matcher.group(1)));
                                        actionRequest.setOutput(Integer.parseInt(matcher.group(2)));
                                    }
                                }
                                RCAction newAction = new RCAction();
                                newAction.setAction(actionRequest.getAction());
                                newAction.setSlaveId(actionRequest.getSlaveId());
                                newAction.setOutput(actionRequest.getOutput());
                                newAction.setDuration(actionRequest.getDuration());
                                newAction.setOrder(actionRequest.getOrder());
                                newAction.setEvent(newEvent);
                                actionsToInsert.add(newAction);
                            }
                            if (eventRequest.getAcls() != null) {
                                List<RCAcl> aclsToInsert = new ArrayList<>();
                                for (RCAcl aclRequest : eventRequest.getAcls()) {
                                    RCAcl newAcl = new RCAcl();
                                    newAcl.setId(aclRequest.getId());
                                    newAcl.setIo(aclRequest.getIo());
                                    newAcl.setState(aclRequest.getState());
                                    newAcl.setType(aclRequest.getType());
                                    newAcl.setEvent(newEvent);
                                    aclsToInsert.add(newAcl);
                                }
                                newEvent.setAcls(aclsToInsert);
                            }
                            newEvent.setActions(actionsToInsert);


                        } else {
                            // существующее событие, придется сравнивать экшены и асл
                            // сравниваем экшены. Сначала добавляем новые экшены
                            List<RCAction> existingActions = existingEvent.get().getActions();
                            List<RCAction> actionsToUpdate = new ArrayList<>();
                            for (RCAction actionRequest : eventRequest.getActions()) {
                                Optional<RCAction> existingAction = existingActions.stream()
                                        .filter(action -> action.getOrder().equals(actionRequest.getOrder()))
                                        .findFirst();
                                // Может быть найдено действие, а может и нет
                                // если нет то просто добавляем, если есть обновляем
                                RCAction newAction = existingAction.orElseGet(RCAction::new);
                                newAction.setAction(actionRequest.getAction());
                                newAction.setSlaveId(actionRequest.getSlaveId());
                                newAction.setOutput(actionRequest.getOutput());
                                newAction.setOrder(actionRequest.getOrder());
                                newAction.setDuration(actionRequest.getDuration());
                                newAction.setEvent(newEvent);
                                actionsToUpdate.add(newAction);
                            }
                            // удаление неиспользуемых экшенов
                            List<RCAction> actionsToDelete = existingActions.stream()
                                    .filter(action -> actionsToUpdate.stream()
                                            .noneMatch(r -> r.getOrder().equals(action.getOrder())))
                                    .collect(Collectors.toList());
                            actionsToDelete.forEach(rcAction -> rcActionRepository.deleteRCAction(rcAction.getUuid()));

                            newEvent.setActions(actionsToUpdate);
                            // ACLs
                            List<RCAcl> existingAcls = existingEvent.map(RCEvent::getAcls).orElseGet(ArrayList::new);
                            List<RCAcl> aclsToUpdate = new ArrayList<>();
                            List<RCAcl> requestAcls = Optional.ofNullable(eventRequest.getAcls())
                                    .orElseGet(ArrayList::new);
                            for (RCAcl aclRequest : requestAcls) {
                                Optional<RCAcl> existingAcl = existingAcls.stream()
                                        .filter(acl -> acl.getCompareId().equals(aclRequest.getCompareId()))
                                        .findFirst();
                                // Может быть найдено действие, а может и нет
                                // если нет то просто добавляем, если есть обновляем
                                RCAcl newAcl = existingAcl.orElseGet(RCAcl::new);
                                newAcl.setEvent(newEvent);
                                newAcl.setState(aclRequest.getState());
                                newAcl.setIo(aclRequest.getIo());
                                newAcl.setId(aclRequest.getId());
                                newAcl.setType(aclRequest.getType());
                                aclsToUpdate.add(newAcl);
                            }
                            // Удаление неиспользуемых ACL
                            List<RCAcl> aclsToDelete = existingAcls.stream()
                                    .filter(acl -> aclsToUpdate.stream()
                                            .noneMatch(r -> r.getCompareId().equals(acl.getCompareId())))
                                    .toList();
                            aclsToDelete.forEach(rcAcl -> rcAclRepository.deleteRCAcl(rcAcl.getUuid()));

                            newEvent.setAcls(aclsToUpdate);
                        }
                        eventsToUpdate.add(newEvent);
                    }

                    // Удаление лишних событий у входа
                    List<RCEvent> eventsToDelete = existingEvents.stream()
                            .filter(event -> eventsToUpdate.stream()
                                    .noneMatch(r -> r.getEvent().equals(event.getEvent())))
                            .collect(Collectors.toList());

                    rcEventRepository.deleteAll(eventsToDelete);
                    eventsToDelete.forEach(rcEvent -> rcEventRepository.deleteEvent(rcEvent.getUuid()));
                    rcInput.setEvents(eventsToUpdate);
                    inputRepository.save(rcInput);
                }
                result = true;
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        }
        return result;
    }
    * */

    public String getDeviceActionMessage(Integer output, String action, Integer slaveId) {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "ACTION");
        objectMap.put("payload", new HashMap<String, Object>() {{
            put("output", output);
            put("action", action);
            if (slaveId > 0)
                put("slaveId", slaveId);
        }});
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(objectMap);
            logger.info(json);
            return json;
        } catch (JsonProcessingException e) {
            //throw new RuntimeException(e);
        }
        return "{}";
    }


}
