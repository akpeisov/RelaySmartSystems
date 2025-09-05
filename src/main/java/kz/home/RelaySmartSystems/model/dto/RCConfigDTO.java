package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCConfigDTO {
    private String mac;
    private String name;
    private String description;
    private String model;
    private RCIOConfigDTO io;
    private RCModbusConfigDTO modbus;
    private NetworkConfigDTO network;
    private RCSchedulerDTO scheduler;
    private RCMqttDTO mqtt;
}
