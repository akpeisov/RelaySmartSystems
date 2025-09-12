package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCConfigDTO {
    private String mac;
    private String name;
    private String description;
    private String model;
    private String status;
    private RCIOConfigDTO io;
    private RCModbusConfigDTO modbus;
    private NetworkConfigDTO network;
    private RCSchedulerDTO scheduler;
    private RCMqttDTO mqtt;
    private long crc;
}