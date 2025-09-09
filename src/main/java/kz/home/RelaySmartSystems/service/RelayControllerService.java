package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.entity.NetworkConfig;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.mapper.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import kz.home.RelaySmartSystems.Utils;

import javax.transaction.Transactional;

@Service
public class RelayControllerService {
    private final RelayControllerRepository relayControllerRepository;
    private final RCOutputRepository outputRepository;
    private final RCInputRepository inputRepository;
    private final RCEventRepository rcEventRepository;
    private final RCActionRepository rcActionRepository;
    private final RCAclRepository rcAclRepository;
    private final RCModbusConfigRepository rcModbusConfigRepository;
    private final RelayControllerMapper relayControllerMapper;
    private final NetworkConfigMapper networkConfigMapper;
    private final NetworkConfigRepository networkConfigRepository;
    private final RCConfigMapper rcConfigMapper;
    private final RCSchedulerMapper rcSchedulerMapper;
    private final RCSchedulerRepository rcSchedulerRepository;
    private final RCMqttMapper rcMqttMapper;
    private final RCMqttRepository rcMqttRepository;
    private static final Logger logger = LoggerFactory.getLogger(RelayControllerService.class);

    public RelayControllerService(RelayControllerRepository relayControllerRepository,
                                  RCOutputRepository outputRepository,
                                  RCInputRepository inputRepository,
                                  RCEventRepository rcEventRepository,
                                  RCActionRepository rcActionRepository,
                                  RCAclRepository rcAclRepository,
                                  RCModbusConfigRepository rcModbusConfigRepository,
                                  ControllerService controllerService,
                                  RelayControllerMapper relayControllerMapper,
                                  NetworkConfigMapper networkConfigMapper,
                                  NetworkConfigRepository networkConfigRepository,
                                  RCConfigMapper rcConfigMapper,
                                  RCSchedulerMapper rcSchedulerMapper,
                                  RCSchedulerRepository rcSchedulerRepository,
                                  RCMqttMapper rcMqttMapper,
                                  RCMqttRepository rcMqttRepository) {
        this.relayControllerRepository = relayControllerRepository;
        this.outputRepository = outputRepository;
        this.inputRepository = inputRepository;
        this.rcEventRepository = rcEventRepository;
        this.rcActionRepository = rcActionRepository;
        this.rcAclRepository = rcAclRepository;
        this.rcModbusConfigRepository = rcModbusConfigRepository;
        this.relayControllerMapper = relayControllerMapper;
        this.networkConfigMapper = networkConfigMapper;
        this.networkConfigRepository = networkConfigRepository;
        this.rcConfigMapper = rcConfigMapper;
        this.rcSchedulerMapper = rcSchedulerMapper;
        this.rcSchedulerRepository = rcSchedulerRepository;
        this.rcMqttMapper = rcMqttMapper;
        this.rcMqttRepository = rcMqttRepository;
    }

    @Transactional
    public void updateRelayControllerIOStates(RCUpdateIODTO rcUpdateIODTO) {
        RelayController relayController = relayControllerRepository.findByMac(rcUpdateIODTO.getMac());
        if (relayController == null) {
            // контролер не найден
            logger.error("Relay controller with mac {} not found", rcUpdateIODTO.getMac());
            return;
        }

        for (RCUpdateIODTO.RCState out : rcUpdateIODTO.getOutputs()) {
            relayController.getOutputs().stream()
                    .filter(child -> child.getId().equals(out.getId()))
                    .filter(child -> child.getSlaveId().equals(out.getSlaveId()))
                    .findFirst()
                    .ifPresent(child -> {
                        child.setState(out.getState());
                    });
        }
        for (RCUpdateIODTO.RCState in : rcUpdateIODTO.getInputs()) {
            relayController.getInputs().stream()
                    .filter(child -> child.getId().equals(in.getId()))
                    .filter(child -> child.getSlaveId().equals(in.getSlaveId()))
                    .findFirst()
                    .ifPresent(child -> {
                        child.setState(in.getState());
                    });
        }
        relayControllerRepository.save(relayController);
    }

    public void setOutputState(String mac, Integer output, String state, Integer slaveId) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCOutput o = outputRepository.findOutput(c.getUuid(), output, slaveId);
            if (o != null) {
                o.setState(state);
                outputRepository.save(o);
            }
        }
    }

    public void setInputState(String mac, Integer input, String state, Integer slaveId) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            RCInput o = inputRepository.findInput(c.getUuid(), input, slaveId);
            if (o != null) {
                o.setState(state);
                inputRepository.save(o);
            }
        }
    }

    // отдельные сервисы сохранения для модбас, сети, mqtt
    public void saveMBConfig(RCModbusConfigDTO rcModbusConfigDTO, String mac) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        saveMBConfig(rcModbusConfigDTO, relayController);
    }

    public void saveNetworkConfig(NetworkConfigDTO networkConfigDTO) {
        RelayController relayController = relayControllerRepository.findByMac(networkConfigDTO.getMac());
        saveNetworkConfig(networkConfigDTO, relayController);
    }

    public void saveSchedulerConfig(RCSchedulerDTO rcSchedulerDTO) {
        RelayController relayController = relayControllerRepository.findByMac(rcSchedulerDTO.getMac());
        saveSchedulerConfig(rcSchedulerDTO, relayController);
    }

    public void saveMqttConfig(RCMqttDTO rcMqttDTO) {
        RelayController relayController = relayControllerRepository.findByMac(rcMqttDTO.getMac());
        saveMqttConfig(rcMqttDTO, relayController);
    }

    private void saveMqttConfig(RCMqttDTO rcMqttDTO,
                                RelayController relayController) {
        if (rcMqttDTO == null)
            return;
        RCMqtt rcMqtt = rcMqttMapper.toEntity(rcMqttDTO);
        rcMqtt.setController(relayController);
        rcMqttRepository.save(rcMqtt);
    }

    private void saveSchedulerConfig(RCSchedulerDTO rcSchedulerDTO,
                                     RelayController relayController) {
        if (rcSchedulerDTO == null)
            return;
        RCScheduler rcScheduler = rcSchedulerMapper.toEntity(rcSchedulerDTO);
        rcScheduler.setController(relayController);
        rcSchedulerRepository.save(rcScheduler);
    }

    private void saveNetworkConfig(NetworkConfigDTO networkConfigDTO,
                                   RelayController relayController) {
        NetworkConfig networkConfig = networkConfigMapper.fromDto(networkConfigDTO);
        networkConfig.setController(relayController);
        networkConfigRepository.save(networkConfig);
    }

    private void saveMBConfig(RCModbusConfigDTO rcModbusConfigDTO,
                             RelayController relayController) {
        if (rcModbusConfigDTO == null)
            return;

        RCModbusConfig rcModbusConfig = RelayControllerMapper.mbConfigToEntity(rcModbusConfigDTO);
        rcModbusConfig.setController(relayController);
        rcModbusConfigRepository.save(rcModbusConfig);

        if ("master".equalsIgnoreCase(rcModbusConfigDTO.getMode())) {
            // find all slaves
            List<Integer> slaveIds = new ArrayList<>();
            for (RCModbusConfig modbusConfig : rcModbusConfigRepository.findByMaster(relayController.getMac())) {
                slaveIds.add(modbusConfig.getSlaveId());
                // add outputs and inputs
                List<RCOutput> rcOutputs = new ArrayList<>();
                for (RCOutput rcOutputSlave : modbusConfig.getController().getOutputs()) {
                    RCOutput rcOutput = new RCOutput();
                    rcOutput.setRelayController(relayController);
                    rcOutput.setSlaveId(modbusConfig.getSlaveId());
                    rcOutput.setId(rcOutputSlave.getId());
                    rcOutput.setName(rcOutputSlave.getName());
                    rcOutput.setType(rcOutputSlave.getType());
                    rcOutputs.add(rcOutput);
                }
                List<RCOutput> existedOutputs = relayController.getOutputs();
                existedOutputs.addAll(rcOutputs);
                relayController.setOutputs(existedOutputs);
            }

            // remove all not in slaveIds
            relayController.getInputs()
                    .removeIf(input -> input.getSlaveId() > 0 && !slaveIds.contains(input.getSlaveId()));
            relayController.getOutputs()
                    .removeIf(output -> output.getSlaveId() > 0 && !slaveIds.contains(output.getSlaveId()));

            relayControllerRepository.save(relayController);

            // хорошо бы сохранить локальные правила на слейвах
        } else {
            // у слейвов или без modbus не может быть дочерних IO
            relayController.getInputs().removeIf(input -> input.getSlaveId() > 0);
            relayController.getOutputs().removeIf(output -> output.getSlaveId() > 0);
            relayControllerRepository.save(relayController);
        }

    }

    public void saveRelayController(RCConfigDTO rcConfigDTO) throws InvocationTargetException, IllegalAccessException {
        // only for new RC
        // find existing RC
        String mac = rcConfigDTO.getMac();
        if (mac == null) {
            logger.error("saveRelayController. Mac is null");
            return;
        }
        RelayController existingRelayController = relayControllerRepository.findByMac(mac);
        if (existingRelayController != null) {
            // rc found. delete it
            logger.info("found existed relayController. deleting it");
            relayControllerRepository.delete(existingRelayController);
        }

        RelayController relayController = relayControllerMapper.toEntity(rcConfigDTO); //rcConfigMapper.toEntityRC(rcConfigDTO);
        relayControllerRepository.save(relayController);

        // modbus
        saveMBConfig(rcConfigDTO.getModbus(), relayController);

        // network
        saveNetworkConfig(rcConfigDTO.getNetwork(), relayController);

        // scheduler
        saveSchedulerConfig(rcConfigDTO.getScheduler(), relayController);

        // mqtt
        saveMqttConfig(rcConfigDTO.getMqtt(), relayController);
    }

    public String makeDeviceConfig(String mac) {
        // формирование конфигурации устройства для отправки на устройство
        String json = "{}";
        try {
            RelayController controller = relayControllerRepository.findByMac(mac.toUpperCase());
            if (controller != null) {

                ModelMapper modelMapper = new ModelMapper();
                RCConfigDTO relayControllerDTO = rcConfigMapper.RCtoDto(controller);

                Map<String, Object> objectMap = new HashMap<>();
                objectMap.put("type", "SETDEVICECONFIG");
                objectMap.put("payload", relayControllerDTO);

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    json = objectMapper.writeValueAsString(objectMap);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            // TODO : в DTO и так не будет лишних id...
            json = Utils.removeFieldsJSON(json, "alice", "timer", "outputID", "mac", "firstDate", "linkDate", "status", "uptime", "freeMemory", "version", "linked", "lastSeen", "wifirssi");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
        return json;
    }

    public User getUser(String mac) {
        RelayController c = relayControllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            return c.getUser();
        }
        return null;
    }

    public String updateOutput(RCUpdateOutputDTO rcUpdateOutputDTO) {
        // обновление сущности выхода с фронта
        Optional<RCOutput> rcOutputOpt = outputRepository.findById(rcUpdateOutputDTO.getUuid());

        if (rcOutputOpt.isPresent()) {
            RCOutput rcOutput = rcOutputOpt.get();
            rcOutput.setName(rcUpdateOutputDTO.getName());
            rcOutput.setAlice(rcUpdateOutputDTO.getAlice());
            rcOutput.setRoom(rcUpdateOutputDTO.getRoom());
            rcOutput.setTimer(rcUpdateOutputDTO.getTimer());
            rcOutput.setOff(rcUpdateOutputDTO.getOff());
            rcOutput.setOn(rcUpdateOutputDTO.getOn());
            rcOutput.setType(rcUpdateOutputDTO.getType());
            rcOutput.setLimit(rcUpdateOutputDTO.getLimit());
            rcOutput.set_default(rcUpdateOutputDTO.get_default());
            outputRepository.save(rcOutput);
            return "OK";
        } else {
            return "Output not found";
        }
    }

    public String updateInput(RCUpdateInputDTO rcUpdateInputDTO) {
        // обновление сущности входа с фронта
        String result = "OK";

        Optional<RCInput> rcInputOpt = inputRepository.findById(rcUpdateInputDTO.getUuid());
        try {
            if (rcInputOpt.isPresent()) {
                RCInput rcInput = rcInputOpt.get();
                if (rcUpdateInputDTO.getName() != null)
                    rcInput.setName(rcUpdateInputDTO.getName());
                if (rcUpdateInputDTO.getType() != null)
                    rcInput.setType(rcUpdateInputDTO.getType());

                // Обработка событий
                List<RCEvent> existingEvents = rcInput.getEvents();
                List<RCEvent> eventsToUpdate = new ArrayList<>();

                // идем по событиям из запроса
                for (RCEvent eventRequest : rcUpdateInputDTO.getEvents()) {
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
                                .toList();
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
            } else {
                return "Not found";
            }
        } catch (Exception e) {
            logger.error("Error while update input", e);
            result = e.getLocalizedMessage();
        }
        return result;
    }

    public String getDeviceActionMessage(Integer output, String action, Integer slaveId) {
        // for alice
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

    @Transactional
    public String getIOStates(String mac) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        if (relayController == null)
            return "{}";
        RCUpdateIODTO rcUpdateIODTO = relayControllerMapper.getRCStates(relayController);

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "UPDATE");
        objectMap.put("payload", rcUpdateIODTO);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(objectMap);
            logger.info(json);
            return json;
        } catch (JsonProcessingException e) {
            logger.error("getIOStates. {}", e.getLocalizedMessage());
        }
        return "{}";
    }
 }
