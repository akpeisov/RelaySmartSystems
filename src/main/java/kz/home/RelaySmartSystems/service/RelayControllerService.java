package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.NetworkConfig;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.mapper.NetworkConfigMapper;
import kz.home.RelaySmartSystems.model.mapper.RCConfigMapper;
import kz.home.RelaySmartSystems.model.mapper.RCSchedulerMapper;
import kz.home.RelaySmartSystems.model.mapper.RelayControllerMapper;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
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
                                  RCSchedulerRepository rcSchedulerRepository) {
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
    }

    @Transactional
    public void updateRelayControllerIOStates(String mac, RCUpdateIODTO rcUpdateIODTO) {
        RelayController relayController = relayControllerRepository.findByMac(mac);
        if (relayController == null) {
            // контролер не найден
            logger.error("Relay controller with mac {} not found", mac);
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

    public void saveRelayController(RCConfigDTO rcConfigDTO) throws InvocationTargetException, IllegalAccessException {
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
        if (rcConfigDTO.getModbus() != null) {
            RCModbusConfig rcModbusConfig = getRcModbusConfig(rcConfigDTO, relayController);
            rcModbusConfigRepository.save(rcModbusConfig);
        }

        // network
        NetworkConfig networkConfig = networkConfigMapper.fromDto(rcConfigDTO.getNetwork());
        networkConfig.setController(relayController);
        networkConfigRepository.save(networkConfig);

        // scheduler
        RCScheduler rcScheduler = rcSchedulerMapper.toEntity(rcConfigDTO.getScheduler());
        rcScheduler.setController(relayController);
        rcSchedulerRepository.save(rcScheduler);

        // mqtt TODO : do it
    }

    private static RCModbusConfig getRcModbusConfig(RCConfigDTO rcConfigDTO, RelayController relayController) {
        RCModbusConfig rcModbusConfig = new RCModbusConfig();
        rcModbusConfig.setController(relayController);
        RCModbusConfigDTO rcModbusInfoDTO = rcConfigDTO.getModbus();
        if ("master".equalsIgnoreCase(rcModbusInfoDTO.getMode())) {
            rcModbusConfig.setMode(ModbusMode.master);
            rcModbusConfig.setMaxRetries(rcModbusInfoDTO.getMaxRetries());
            rcModbusConfig.setPollingTime(rcModbusInfoDTO.getPollingTime());
            rcModbusConfig.setReadTimeout(rcModbusInfoDTO.getReadTimeout());
            rcModbusConfig.setActionOnSameSlave(rcModbusInfoDTO.getActionOnSameSlave());
        } else if ("slave".equalsIgnoreCase(rcModbusInfoDTO.getMode())) {
            rcModbusConfig.setMode(ModbusMode.slave);
            rcModbusConfig.setSlaveId(rcModbusInfoDTO.getSlaveId());
            rcModbusConfig.setMaster(rcModbusInfoDTO.getMaster());
        }
        //BeanUtils.copyProperties(rcModbusConfig, rcConfigDTO.getModbus());
        rcModbusConfig.setController(relayController);
        return rcModbusConfig;
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
 }
