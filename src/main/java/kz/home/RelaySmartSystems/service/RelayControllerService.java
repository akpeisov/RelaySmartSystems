package kz.home.RelaySmartSystems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.home.RelaySmartSystems.model.entity.*;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.mapper.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

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

    @Transactional
    public String saveConfig(RCConfigDTO rcConfigDTO) {
        RelayController relayController = relayControllerRepository.findByMac(rcConfigDTO.getMac());
        String res = saveMBConfig(rcConfigDTO.getModbus(), relayController);
        if (!"OK".equals(res))
            return res;
        res = saveNetworkConfig(rcConfigDTO.getNetwork(), relayController);
        if (!"OK".equals(res))
            return res;
        res = saveSchedulerConfig(rcConfigDTO.getScheduler(), relayController);
//        saveMqttConfig(rcConfigDTO.getMqtt(), relayController);
        return res;
    }

    private void saveMqttConfig(RCMqttDTO rcMqttDTO,
                                RelayController relayController) {
        if (rcMqttDTO == null)
            return;
        RCMqtt rcMqtt = rcMqttMapper.toEntity(rcMqttDTO);
        rcMqtt.setController(relayController);
        rcMqttRepository.save(rcMqtt);
    }

    String saveSchedulerConfig(RCSchedulerDTO rcSchedulerDTO,
                               RelayController relayController) {
        if (rcSchedulerDTO == null || relayController == null)
            return "NULL";
        try {
            RCScheduler existingScheduler = rcSchedulerRepository.findByController(relayController);
            if (existingScheduler != null) {
                // update existing
                existingScheduler.setEnabled(rcSchedulerDTO.isEnabled());

                // --- синхронизация задач ---
                List<RCTask> existingTasks = existingScheduler.getTasks();
                List<RCSchedulerDTO.RCTaskDTO> dtoTasks = rcSchedulerDTO.getTasks() != null ? rcSchedulerDTO.getTasks() : new ArrayList<>();

                // Удаляем задачи, которых нет в DTO (по имени)
                existingTasks.removeIf(task -> dtoTasks.stream().noneMatch(dto -> dto.getName() != null && dto.getName().equals(task.getName())));

                // Обновляем существующие и добавляем новые задачи
                for (RCSchedulerDTO.RCTaskDTO dtoTask : dtoTasks) {
                    RCTask task = existingTasks.stream()
                        .filter(t -> dtoTask.getName() != null && dtoTask.getName().equals(t.getName()))
                        .findFirst()
                        .orElse(null);
                    if (task == null) {
                        // новая задача
                        task = new RCTask();
                        task.setScheduler(existingScheduler);
                        existingTasks.add(task);
                    }
                    // обновляем поля задачи
                    task.setName(dtoTask.getName());
                    task.setGrace(dtoTask.getGrace());
                    task.setTime(dtoTask.getTime());
                    task.setDone(dtoTask.isDone());
                    task.setEnabled(dtoTask.isEnabled());
                    task.setDow(dtoTask.getDow());

                    // --- синхронизация действий задачи ---
                    List<RCTaskAction> existingActions = task.getActions();
                    List<RCSchedulerDTO.RCTaskActionDTO> dtoActions = dtoTask.getActions() != null ? dtoTask.getActions() : new ArrayList<>();
                    // Удаляем действия, которых нет в DTO (по action и output)
                    existingActions.removeIf(act -> dtoActions.stream().noneMatch(dto -> Objects.equals(dto.getAction(), act.getAction()) && Objects.equals(dto.getOutput(), act.getOutput())));
                    // Обновляем существующие и добавляем новые действия
                    for (RCSchedulerDTO.RCTaskActionDTO dtoAction : dtoActions) {
                        RCTaskAction action = existingActions.stream()
                            .filter(a -> Objects.equals(dtoAction.getAction(), a.getAction()) && Objects.equals(dtoAction.getOutput(), a.getOutput()))
                            .findFirst()
                            .orElse(null);
                        if (action == null) {
                            action = new RCTaskAction();
                            action.setTask(task);
                            existingActions.add(action);
                        }
                        action.setAction(dtoAction.getAction());
                        action.setOutput(dtoAction.getOutput());
                        action.setType(dtoAction.getType());
                        action.setInput(dtoAction.getInput());
                    }
                }
                rcSchedulerRepository.save(existingScheduler);
                return "OK";
            }

            // если нет существующего планировщика, создаём новый
            RCScheduler rcScheduler = rcSchedulerMapper.toEntity(rcSchedulerDTO);
            rcScheduler.setController(relayController);
            rcSchedulerRepository.save(rcScheduler);
        } catch (Exception e) {
            logger.error("Error while saving scheduler config", e);
            return e.getLocalizedMessage();
        }
        return "OK";
    }

    private String saveNetworkConfig(NetworkConfigDTO networkConfigDTO,
                                   RelayController relayController) {
        if (relayController == null || networkConfigDTO == null) {
            return "EMPTY";
        }
        NetworkConfig existingConfig = networkConfigRepository.findByController(relayController);
        /*
        NetworkConfig existingConfig = networkConfigRepository.findAll().stream()
            .filter(cfg -> cfg.getController() != null && cfg.getController().getUuid().equals(relayController.getUuid()))
            .findFirst().orElse(null);*/

        if (existingConfig != null) {
            // Обновляем поля
            existingConfig.setNtpServer(networkConfigDTO.getNtpServer());
            existingConfig.setNtpTZ(networkConfigDTO.getNtpTZ());
            existingConfig.setOtaURL(networkConfigDTO.getOtaURL());

            // Обновляем вложенные сущности
            if (networkConfigDTO.getCloud() != null) {
                if (existingConfig.getCloud() == null) {
                    CloudConfig config = new CloudConfig();
                    networkConfigMapper.cloudFromDto(config, networkConfigDTO.getCloud());
                    existingConfig.setCloud(config);
                } else {
                    networkConfigMapper.cloudFromDto(existingConfig.getCloud(), networkConfigDTO.getCloud());
                }
            } else {
                existingConfig.setCloud(null);
            }
            if (networkConfigDTO.getEth() != null) {
                if (existingConfig.getEth() == null) {
                    EthConfig ethConfig = new EthConfig();
                    networkConfigMapper.ethFromDto(ethConfig, networkConfigDTO.getEth());
                    existingConfig.setEth(ethConfig);
                } else {
                    networkConfigMapper.ethFromDto(existingConfig.getEth(), networkConfigDTO.getEth());
                }
            } else {
                existingConfig.setEth(null);
            }
            if (networkConfigDTO.getWifi() != null) {
                if (existingConfig.getWifi() == null) {
                    WifiConfig wifiConfig = new WifiConfig();
                    networkConfigMapper.wifiFromDto(wifiConfig, networkConfigDTO.getWifi());
                    existingConfig.setWifi(wifiConfig);
                } else {
                    networkConfigMapper.wifiFromDto(existingConfig.getWifi(), networkConfigDTO.getWifi());
                }
            } else {
                existingConfig.setWifi(null);
            }
            if (networkConfigDTO.getFtp() != null) {
                if (existingConfig.getFtp() == null) {
                    FtpConfig ftpConfig = new FtpConfig();
                    networkConfigMapper.ftpFromDto(ftpConfig, networkConfigDTO.getFtp());
                    existingConfig.setFtp(ftpConfig);
                } else {
                    networkConfigMapper.ftpFromDto(existingConfig.getFtp(), networkConfigDTO.getFtp());
                }
            } else {
                existingConfig.setFtp(null);
            }
            networkConfigRepository.save(existingConfig);
        } else {
            NetworkConfig networkConfig = networkConfigMapper.fromDto(networkConfigDTO);
            networkConfig.setController(relayController);
            networkConfigRepository.save(networkConfig);
        }
        return "OK";
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

    private String saveMBConfig(RCModbusConfigDTO rcModbusConfigDTO,
                             RelayController relayController) {
        if (rcModbusConfigDTO == null)
            return "EMPTY";

        UUID rcModbusConfigUUID;
        RCModbusConfig config = rcModbusConfigRepository.findByController(relayController);
        if (config != null) {
            rcModbusConfigUUID = config.getUuid();
        } else {
            rcModbusConfigUUID = null;
        }

        // для слейва надо проверить есть ли другой слейв с таким же slaveId
        if ("slave".equalsIgnoreCase(rcModbusConfigDTO.getMode())) {
            if (rcModbusConfigRepository.findByMaster(rcModbusConfigDTO.getMaster()).stream()
                    .filter(mb -> !mb.getUuid().equals(rcModbusConfigUUID))
                    .anyMatch(mb -> mb.getSlaveId().equals(rcModbusConfigDTO.getSlaveId()))) {
                return "Another slave with same slaveId already exists";
            }
        }
        RCModbusConfig rcModbusConfig = RelayControllerMapper.mbConfigToEntity(rcModbusConfigDTO);
        rcModbusConfig.setUuid(rcModbusConfigUUID);
        rcModbusConfig.setController(relayController);
        rcModbusConfigRepository.save(rcModbusConfig);

        if ("master".equalsIgnoreCase(rcModbusConfigDTO.getMode())) {
            // find all slaves and add their io to master
            List<Integer> slaveIds = new ArrayList<>(); // list of slaves
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
//            relayController.getInputs()
//                    .removeIf(input -> input.getSlaveId() > 0 && !slaveIds.contains(input.getSlaveId()));
            relayController.getOutputs()
                    .removeIf(output -> output.getSlaveId() > 0 && !slaveIds.contains(output.getSlaveId()));

            relayControllerRepository.save(relayController);
        } else {
            if ("slave".equalsIgnoreCase(rcModbusConfigDTO.getMode())) {
                // TODO : если slaveId поменять то будет жопа, надо исправить, т.к. существующие io у мастера останутся и добавятся новые
                // find master
                RelayController master = relayControllerRepository.findByMac(rcModbusConfig.getMaster());
                if (master == null) {
                    logger.error("Master controller with mac {} not found", rcModbusConfig.getMaster());
                    return "Master controller not found";
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
            // у слейвов или без modbus не может быть дочерних IO
            relayController.getInputs().removeIf(input -> input.getSlaveId() > 0);
            relayController.getOutputs().removeIf(output -> output.getSlaveId() > 0);
            relayControllerRepository.save(relayController);
        }
        return "OK";
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
        //saveMBConfig(rcConfigDTO.getModbus(), relayController);

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
                json = Utils.removeFieldsJSON(json, "uuid", "alice", "timer", "outputID", "mac", "firstDate", "linkDate", "status", "uptime", "freeMemory", "version", "linked", "lastSeen", "wifirssi");
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

    @Transactional
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

    @Transactional
    public String updateInput(RCInputDTO rcInputDTO) {
        String result = "OK";
        Optional<RCInput> rcInputOpt = inputRepository.findById(rcInputDTO.getUuid());
        try {
            if (rcInputOpt.isPresent()) {
                RCInput rcInput = rcInputOpt.get();
                rcInput.setName(rcInputDTO.getName());
                rcInput.setType(rcInputDTO.getType());
                rcInput.setSlaveId(rcInputDTO.getSlaveId());

                if (!rcInput.getCRC().equals(rcInputDTO.getCRC())) {
                    // events changed
                    rcInput.getEvents().clear();

                    if (rcInputDTO.getEvents() != null) {
                        for (RCEventDTO eventDto : rcInputDTO.getEvents()) {
                            RCEvent event = new RCEvent();
                            event.setEvent(eventDto.getEvent());
                            event.setInput(rcInput);

                            // actions
                            if (eventDto.getActions() != null) {
                                List<RCAction> actions = new ArrayList<>();
                                for (RCActionDTO actionDto : eventDto.getActions()) {
                                    RCAction action = getActionFromDto(actionDto);
                                    action.setEvent(event);
                                    actions.add(action);
                                }
                                event.setActions(actions);
                            }
                            // acls
                            if (eventDto.getAcls() != null) {
                                List<RCAcl> acls = new ArrayList<>();
                                for (RCAclDTO aclDto : eventDto.getAcls()) {
                                    RCAcl acl = getAclFromDto(aclDto);
                                    acl.setEvent(event);
                                    acls.add(acl);
                                }
                                event.setAcls(acls);
                            }
                            rcInput.getEvents().add(event);
                        }
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

    private static RCAction getActionFromDto(RCActionDTO actionDto) {
        RCAction action = new RCAction();
        action.setOrder(actionDto.getOrder());
        action.setOutput(actionDto.getOutput());
        action.setAction(actionDto.getAction());
        action.setDuration(actionDto.getDuration());
        action.setSlaveId(actionDto.getSlaveId());
        return action;
    }

    private static RCAcl getAclFromDto(RCAclDTO aclDto) {
        RCAcl acl = new RCAcl();
        acl.setType(aclDto.getType());
        acl.setId(aclDto.getId());
        acl.setIo(aclDto.getIo());
        acl.setState(aclDto.getState());
        return acl;
    }

    public String getDeviceActionMessage(RCOutput output, String action) {
        // for alice
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "ACTION");
        objectMap.put("payload", new HashMap<String, Object>() {{
            put("output", output.getId());
            put("action", action);
            if (output.getSlaveId() > 0)
                put("slaveId", output.getSlaveId());
        }});
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(objectMap);
            logger.info(json);
            return json;
        } catch (JsonProcessingException e) {
            logger.error(e.getLocalizedMessage());
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
