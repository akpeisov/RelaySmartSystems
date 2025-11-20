package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.entity.NetworkConfig;
import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.entity.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RCModbusConfigRepository;
import kz.home.RelaySmartSystems.repository.RCOutputRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RCConfigMapper {
    private final RelayControllerMapper relayControllerMapper;
    private final RCModbusConfigRepository modbusConfigRepository;
    private final RCSchedulerMapper rcSchedulerMapper;
    private final RCMqttMapper rcMqttMapper;
    public RCConfigMapper(RelayControllerMapper relayControllerMapper,
                          RCModbusConfigRepository modbusConfigRepository,
                          RCSchedulerMapper rcSchedulerMapper,
                          RCMqttMapper rcMqttMapper) {
        this.relayControllerMapper = relayControllerMapper;
        this.modbusConfigRepository = modbusConfigRepository;
        this.rcSchedulerMapper = rcSchedulerMapper;
        this.rcMqttMapper = rcMqttMapper;
    }

    private NetworkConfigDTO networkToDto(NetworkConfig networkConfig) {
        if (networkConfig == null)
            return null;
        NetworkConfigDTO networkConfigDTO = new NetworkConfigDTO();
        networkConfigDTO.setNtpTZ(networkConfig.getNtpTZ());
        networkConfigDTO.setNtpServer(networkConfig.getNtpServer());
        networkConfigDTO.setOtaURL(networkConfig.getOtaURL());
        // cloud
        if (networkConfig.getCloud() != null) {
            NetworkConfigDTO.CloudDto cloudDto = new NetworkConfigDTO.CloudDto();
            cloudDto.setAddress(networkConfig.getCloud().getAddress());
            cloudDto.setEnabled(networkConfig.getCloud().isEnabled());
            networkConfigDTO.setCloud(cloudDto);
        }
        // ftp
        if (networkConfig.getFtp() != null) {
            NetworkConfigDTO.FtpDto ftpDto = new NetworkConfigDTO.FtpDto();
            ftpDto.setUser(networkConfig.getFtp().getUser());
            ftpDto.setPass(networkConfig.getFtp().getPass());
            ftpDto.setEnabled(networkConfig.getFtp().isEnabled());
            networkConfigDTO.setFtp(ftpDto);
        }
        // eth
        networkConfigDTO.setEth(getEthDto(networkConfig));
        // wifi
        networkConfigDTO.setWifi(getWifiDto(networkConfig));

        return networkConfigDTO;
    }

    private static NetworkConfigDTO.EthDto getEthDto(NetworkConfig networkConfig) {
        if (networkConfig.getEth() == null)
            return null;
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
        if (networkConfig.getWifi() == null)
            return null;
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

    public RCConfigDTO RCtoDto(RelayController controller) {
        RCConfigDTO rcConfigDTO = new RCConfigDTO();
        // general info
        rcConfigDTO.setName(controller.getName());
        rcConfigDTO.setMac(controller.getMac());
        rcConfigDTO.setDescription(controller.getDescription());
        rcConfigDTO.setModel(controller.getModel());
        rcConfigDTO.setStatus(controller.getStatus());
        rcConfigDTO.setLastSeen(controller.getLastSeen());
        rcConfigDTO.setHwParams(controller.getHwParams());
        // io
        RCIOConfigDTO rcioConfigDTO = new RCIOConfigDTO();
        rcioConfigDTO.setOutputs(relayControllerMapper.outputsToDTO(controller.getOutputs()));

        // сначала получить inputs DTO
        List<RCInputDTO> inputs = relayControllerMapper.inputsToDTO(controller.getInputs());
        rcioConfigDTO.setInputs(inputs);

        rcConfigDTO.setIo(rcioConfigDTO);
        // modbus
        RCModbusConfigDTO rcModbusInfoDTO = relayControllerMapper.modbusToDTO(controller.getModbusConfig());
        // for master include all slaves
        if (rcModbusInfoDTO != null && "master".equalsIgnoreCase(rcModbusInfoDTO.getMode())) {
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
        // scheduler
        rcConfigDTO.setScheduler(rcSchedulerMapper.toDto(controller.getScheduler()));
        // mqtt
        rcConfigDTO.setMqtt(rcMqttMapper.toDto(controller.getMqtt()));

        rcConfigDTO.setUsername(controller.getUser() != null ? controller.getUser().getUsername() : null);
        return rcConfigDTO;
    }
}
