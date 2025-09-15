package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
    @Transactional
    public void saveMBConfig(RCModbusConfigDTO rcModbusConfigDTO) {
        RelayController relayController = relayControllerRepository.findByMac(rcModbusConfigDTO.getMac());
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

    private List<RCOutput> getNewMBOutputs(List<RCOutput> outputs, RelayController relayController, Integer slaveId) {
        List<RCOutput> rcOutputs = new ArrayList<>();
        for (RCOutput rcOutputSlave : outputs) {
            if (relayController.getOutputs().stream()
                    .anyMatch(rcOutput -> rcOutput.getId().equals(rcOutputSlave.getId()) &&
                            rcOutput.getSlaveId().equals(slaveId))) {
                // skip existing outputs
                continue;
            }
            RCOutput rcOutput = new RCOutput();
            rcOutput.setRelayController(relayController);
            rcOutput.setSlaveId(slaveId);
            rcOutput.setId(rcOutputSlave.getId());
            rcOutput.setName(rcOutputSlave.getName());
            rcOutput.setType(rcOutputSlave.getType());
            rcOutputs.add(rcOutput);
        }
        return rcOutputs;
    }

    private List<RCInput> getNewMBInputs(List<RCInput> inputs, RelayController relayController, Integer slaveId) {
        List<RCInput> rcInputs = new ArrayList<>();
        for (RCInput rcInputSlave : inputs) {
            if (rcInputSlave.getId() > 15)
                continue;
            if (relayController.getInputs().stream()
                    .anyMatch(rcInput -> rcInput.getId().equals(rcInputSlave.getId()) &&
                            rcInput.getSlaveId().equals(slaveId))) {
                // skip existing inputs
                continue;
            }
            RCInput rcInput = new RCInput();
            rcInput.setRelayController(relayController);
            rcInput.setSlaveId(slaveId);
            rcInput.setId(rcInputSlave.getId());
            rcInput.setName(rcInputSlave.getName());
            rcInput.setType(rcInputSlave.getType());
            rcInputs.add(rcInput);
        }
        return rcInputs;
        // TODO : хорошо бы сохранить локальные правила на слейвах
    }

    private void saveMBConfig(RCModbusConfigDTO rcModbusConfigDTO,
                             RelayController relayController) {
        if (rcModbusConfigDTO == null)
            return;

        UUID rcModbusConfigUUID = null;
        RCModbusConfig config = rcModbusConfigRepository.findByController(relayController);
        if (config != null) {
            rcModbusConfigUUID = config.getUuid();
        }
        RCModbusConfig rcModbusConfig = RelayControllerMapper.mbConfigToEntity(rcModbusConfigDTO);
        rcModbusConfig.setUuid(rcModbusConfigUUID);
        rcModbusConfig.setController(relayController);
        rcModbusConfigRepository.save(rcModbusConfig);

        if ("master".equalsIgnoreCase(rcModbusConfigDTO.getMode())) {
            // find all slaves and add their io to master
            List<Integer> slaveIds = new ArrayList<>();
            for (RCModbusConfig modbusConfig : rcModbusConfigRepository.findByMaster(relayController.getMac())) {
                slaveIds.add(modbusConfig.getSlaveId());
                // add outputs and inputs
                List<RCOutput> rcOutputs = getNewMBOutputs(modbusConfig.getController().getOutputs(), relayController, modbusConfig.getSlaveId());
                List<RCOutput> existedOutputs = relayController.getOutputs();
                existedOutputs.addAll(rcOutputs);
                relayController.setOutputs(existedOutputs);

                List<RCInput> rcInputs = getNewMBInputs(modbusConfig.getController().getInputs(), relayController, modbusConfig.getSlaveId());
                List<RCInput> existedInputs = relayController.getInputs();
                existedInputs.addAll(rcInputs);
                relayController.setInputs(existedInputs);
            }

            // remove all io not in slaveIds
            relayController.getInputs()
                    .removeIf(input -> input.getSlaveId() > 0 && !slaveIds.contains(input.getSlaveId()));
            relayController.getOutputs()
                    .removeIf(output -> output.getSlaveId() > 0 && !slaveIds.contains(output.getSlaveId()));

            relayControllerRepository.save(relayController);
        } else {
            // у слейвов или без modbus не может быть дочерних IO
            relayController.getInputs().removeIf(input -> input.getSlaveId() > 0);
            relayController.getOutputs().removeIf(output -> output.getSlaveId() > 0);
            relayControllerRepository.save(relayController);
            if ("slave".equalsIgnoreCase(rcModbusConfigDTO.getMode())) {
                // TODO : если slaveId поменять то будет жопа, надо исправить, т.к. существующие io у мастера останутся и добавятся новые
                // find master
                RelayController master = relayControllerRepository.findByMac(rcModbusConfig.getMaster());
                if (master == null) {
                    logger.error("Master controller with mac {} not found", rcModbusConfig.getMaster());
                    return;
                }
                List<RCOutput> rcOutputs = getNewMBOutputs(relayController.getOutputs(), master, rcModbusConfig.getSlaveId());
                List<RCOutput> existedOutputs = master.getOutputs();
                existedOutputs.addAll(rcOutputs);
                master.setOutputs(existedOutputs);

                List<RCInput> rcInputs = getNewMBInputs(relayController.getInputs(), master, rcModbusConfig.getSlaveId());
                List<RCInput> existedInputs = master.getInputs();
                existedInputs.addAll(rcInputs);
                master.setInputs(existedInputs);
                relayControllerRepository.save(master);
            }
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

    @Transactional
    public String makeDeviceConfig(String mac) {
        // формирование конфигурации устройства для отправки на устройство
        String json = "{}";
        try {
            RelayController controller = relayControllerRepository.findByMac(mac.toUpperCase());
            if (controller != null) {

                RCConfigDTO relayControllerDTO = rcConfigMapper.RCtoDto(controller);
                relayControllerDTO.setCrc(Utils.getCRC(Utils.getJson(relayControllerDTO)));

                Map<String, Object> objectMap = new HashMap<>();

                objectMap.put("type", "SETDEVICECONFIG");
                objectMap.put("payload", relayControllerDTO);

                json = Utils.getJson(objectMap);
                json = Utils.removeFieldsJSON(json, "alice", "timer", "outputID", "mac", "firstDate", "linkDate", "status", "uptime", "freeMemory", "version", "linked", "lastSeen", "wifirssi");
            }
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

    public String updateOutput(RCOutputDTO rcOutputDTO) {
        // обновление сущности выхода с фронта
        Optional<RCOutput> rcOutputOpt = outputRepository.findById(rcOutputDTO.getUuid());

        if (rcOutputOpt.isPresent()) {
            RCOutput rcOutput = rcOutputOpt.get();
            rcOutput.setName(rcOutputDTO.getName());
            rcOutput.setAlice(rcOutputDTO.getAlice());
            rcOutput.setRoom(rcOutputDTO.getRoom());
            rcOutput.setTimer(rcOutputDTO.getTimer());
            rcOutput.setOff(rcOutputDTO.getOff());
            rcOutput.setOn(rcOutputDTO.getOn());
            rcOutput.setType(rcOutputDTO.getType());
            rcOutput.setLimit(rcOutputDTO.getLimit());
            rcOutput.set_default(rcOutputDTO.get_default());
            outputRepository.save(rcOutput);
            return "OK";
        } else {
            return "Output not found";
        }
    }

    public String updateInput(RCInputDTO rcInputDTO) {
        // обновление сущности входа с фронта
        String result = "OK";

        Optional<RCInput> rcInputOpt = inputRepository.findById(rcInputDTO.getUuid());
        try {
            if (rcInputOpt.isPresent()) {
                RCInput rcInput = rcInputOpt.get();
                rcInput.setName(rcInputDTO.getName());
                rcInput.setType(rcInputDTO.getType());
                rcInput.setSlaveId(rcInputDTO.getSlaveId());

                // === Обновляем events ===
                rcInput.getEvents().clear();
                if (rcInputDTO.getEvents() != null) {
                    for (RCEventDTO eventDto : rcInputDTO.getEvents()) {
                        RCEvent event = new RCEvent();
                        event.setUuid(eventDto.getUuid());
                        event.setEvent(eventDto.getEvent());
                        event.setInput(rcInput);

                        // === Обновляем actions ===
                        if (eventDto.getActions() != null) {
                            List<RCAction> actions = new ArrayList<>();
                            for (RCActionDTO actionDto : eventDto.getActions()) {
                                RCAction action = new RCAction();
                                action.setUuid(actionDto.getUuid());
                                action.setOrder(actionDto.getOrder());
                                action.setOutput(actionDto.getOutput());
                                action.setAction(actionDto.getAction());
                                action.setDuration(actionDto.getDuration());
                                action.setSlaveId(actionDto.getSlaveId());
                                action.setEvent(event);
                                actions.add(action);
                            }
                            event.setActions(actions);
                        }

                        // === Обновляем ACLs ===
                        if (eventDto.getAcls() != null) {
                            List<RCAcl> acls = new ArrayList<>();
                            for (RCAclDTO aclDto : eventDto.getAcls()) {
                                RCAcl acl = new RCAcl();
                                acl.setUuid(aclDto.getUuid());
                                acl.setType(aclDto.getType());
                                acl.setId(aclDto.getId());
                                acl.setIo(aclDto.getIo());
                                acl.setState(aclDto.getState());
                                acl.setEvent(event);
                                acls.add(acl);
                            }
                            event.setAcls(acls);
                        }
                        rcInput.getEvents().add(event);
                    }
                }
                inputRepository.save(rcInput);
            } else {
                return "Input not found";
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
        return Utils.getJson(objectMap);
    }
 }
