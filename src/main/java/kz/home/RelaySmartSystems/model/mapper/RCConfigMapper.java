package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.NetworkConfig;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RCModbusConfigRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RCConfigMapper {
    private final RelayControllerMapper relayControllerMapper;
    private final RCModbusConfigRepository modbusConfigRepository;
    public RCConfigMapper(RelayControllerMapper relayControllerMapper,
                          RCModbusConfigRepository modbusConfigRepository) {
        this.relayControllerMapper = relayControllerMapper;
        this.modbusConfigRepository = modbusConfigRepository;
    }

    private NetworkConfigDTO networkToDto(NetworkConfig networkConfig) {
        if (networkConfig == null)
            return null;
        NetworkConfigDTO rcModbusConfigDTO = new NetworkConfigDTO();
        rcModbusConfigDTO.setNtpTZ(networkConfig.getNtpTZ());
        rcModbusConfigDTO.setNtpServer(networkConfig.getNtpServer());
        rcModbusConfigDTO.setOtaURL(networkConfig.getOtaURL());
        // cloud
        NetworkConfigDTO.CloudDto cloudDto = new NetworkConfigDTO.CloudDto();
        cloudDto.setAddress(networkConfig.getCloud().getAddress());
        cloudDto.setEnabled(networkConfig.getCloud().isEnabled());
        rcModbusConfigDTO.setCloud(cloudDto);
        // ftp
        NetworkConfigDTO.FtpDto ftpDto = new NetworkConfigDTO.FtpDto();
        ftpDto.setUser(networkConfig.getFtp().getUser());
        ftpDto.setPass(networkConfig.getFtp().getPass());
        ftpDto.setEnabled(networkConfig.getFtp().isEnabled());
        rcModbusConfigDTO.setFtp(ftpDto);
        // eth
        rcModbusConfigDTO.setEth(getEthDto(networkConfig));
        // wifi
        rcModbusConfigDTO.setWifi(getWifiDto(networkConfig));

        return rcModbusConfigDTO;        
    }

    private static NetworkConfigDTO.EthDto getEthDto(NetworkConfig networkConfig) {
        NetworkConfigDTO.EthDto ethDto = new NetworkConfigDTO.EthDto();
        ethDto.setIp(networkConfig.getEth().getIp());
        ethDto.setDhcp(networkConfig.getEth().isDhcp());
        ethDto.setDns(networkConfig.getEth().getDns());
        ethDto.setGateway(networkConfig.getEth().getGateway());
        ethDto.setNetmask(networkConfig.getEth().getNetmask());
        ethDto.setEnabled(networkConfig.getEth().isEnabled());
        ethDto.setEnableReset(networkConfig.getEth().isEnableReset());
        ethDto.setResetGPIO(networkConfig.getEth().getResetGPIO());
        return ethDto;
    }

    private static NetworkConfigDTO.WifiDto getWifiDto(NetworkConfig networkConfig) {
        NetworkConfigDTO.WifiDto wifiDto = new NetworkConfigDTO.WifiDto();
        wifiDto.setIp(networkConfig.getWifi().getIp());
        wifiDto.setDhcp(networkConfig.getWifi().isDhcp());
        wifiDto.setGateway(networkConfig.getWifi().getGateway());
        wifiDto.setNetmask(networkConfig.getWifi().getNetmask());
        wifiDto.setEnabled(networkConfig.getWifi().isEnabled());
        wifiDto.setSsid(networkConfig.getWifi().getSsid());
        wifiDto.setPass(networkConfig.getWifi().getPass());
        return wifiDto;
    }

    public RelayController toEntityRC(RCConfigDTO rcConfigDTO) throws InvocationTargetException, IllegalAccessException {
        RelayController relayController = new RelayController();

        // general info
        relayController.setMac(rcConfigDTO.getMac());
        relayController.setName(rcConfigDTO.getName());
        relayController.setDescription(rcConfigDTO.getDescription());
        relayController.setModel(rcConfigDTO.getModel());
        relayController.setType("relayController");

        // outputs
        List<RCOutput> outputs = new ArrayList<>();
        for (RCOutputDTO outputDTO : rcConfigDTO.getIo().getOutputs()) {
            RCOutput output = new RCOutput();
            output.setRelayController(relayController);
            BeanUtils.copyProperties(output, outputDTO);
            outputs.add(output);
        }
        relayController.setOutputs(outputs);

        // inputs
        List<RCInput> inputs = new ArrayList<>();
        for (RCInputDTO inputDTO : rcConfigDTO.getIo().getInputs()) {
            RCInput input = new RCInput();
            input.setRelayController(relayController);
            //BeanUtils.copyProperties(input, inputDTO); // Cannot invoke kz.home.RelaySmartSystems.model.relaycontroller.RCInput.setEvents - argument type mismatch
            input.setId(inputDTO.getId());
            input.setName(inputDTO.getState());
            input.setType(inputDTO.getType());
            input.setState(inputDTO.getState());
            input.setSlaveId(inputDTO.getSlaveId());

            if (inputDTO.getEvents() != null) {
                List<RCEvent> newEvents = new ArrayList<>();
                for (RCEventDTO eventDTO : inputDTO.getEvents()) {
                    RCEvent newEvent = new RCEvent();
                    BeanUtils.copyProperties(newEvent, eventDTO);
                    newEvent.setInput(input);
                    // actions
                    if (eventDTO.getActions() != null) {
                        Set<RCAction> newActions = new HashSet<>();
                        for (RCActionDTO action : eventDTO.getActions()) {
                            RCAction newAction = new RCAction();
                            BeanUtils.copyProperties(newAction, action);
                            newAction.setEvent(newEvent);
                            newActions.add(newAction);
                        }
                        newEvent.setActions(newActions);
                    }
                    // acls
                    if (eventDTO.getAcls() != null) {
                        Set<RCAcl> newAcls = new HashSet<>();
                        for (RCAclDTO acl : eventDTO.getAcls()) {
                            RCAcl newAcl = new RCAcl();
                            BeanUtils.copyProperties(newAcl, acl);
                            newAcl.setEvent(newEvent);
                            newAcls.add(newAcl);
                        }
                        newEvent.setAcls(newAcls);
                    }
                    newEvents.add(newEvent);
                }
                input.setEvents(newEvents);
            }
            inputs.add(input);
        }
        relayController.setInputs(inputs);

        return relayController;
    }

    public RCConfigDTO RCtoDto(RelayController controller) {
        RCConfigDTO rcConfigDTO = new RCConfigDTO();
        // general info
        rcConfigDTO.setName(controller.getName());
        rcConfigDTO.setMac(controller.getMac());
        rcConfigDTO.setDescription(controller.getDescription());
        rcConfigDTO.setModel(controller.getModel());
        // io
        RCIOConfigDTO rcioConfigDTO = new RCIOConfigDTO();
        rcioConfigDTO.setOutputs(relayControllerMapper.outputsToDTO(controller.getOutputs()));
        rcioConfigDTO.setInputs(relayControllerMapper.inputsToDTO(controller.getInputs()));
        rcConfigDTO.setIo(rcioConfigDTO);
        // modbus
        RCModbusConfigDTO rcModbusInfoDTO = relayControllerMapper.modbusToDTO(controller.getModbusConfig());
        // for master include all slaves
        if ("master".equalsIgnoreCase(rcModbusInfoDTO.getMode())) {
            List<RCModbusConfigDTO.SlaveDTO> slaveDTOS = new ArrayList<>();
            for (RCModbusConfig modbusConfig : modbusConfigRepository.findByMaster(controller.getMac())) {
                RCModbusConfigDTO.SlaveDTO slaveDTO = new RCModbusConfigDTO.SlaveDTO();
                slaveDTO.setMac(modbusConfig.getController().getMac());
                slaveDTO.setModel(modbusConfig.getController().getModel());
                slaveDTO.setSlaveId(modbusConfig.getSlaveId());
                slaveDTOS.add(slaveDTO);
            }
            rcModbusInfoDTO.setSlaves(slaveDTOS);
        }
        rcConfigDTO.setModbus(rcModbusInfoDTO);
        // network
        rcConfigDTO.setNetwork(networkToDto(controller.getNetworkConfig()));

        return rcConfigDTO;
    }
}
