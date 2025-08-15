package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RCModbusInfoDTO {
    private String mode; // master | slave
    private Integer pollingTime;
    private Integer readTimeout;
    private Integer maxRetries;
    private Boolean actionOnSameSlave;
    private List<SlaveDTO> slaves;
    private Integer slaveId;
    private UUID master;

    @Getter
    @Setter
    public static class SlaveDTO {
        private UUID uuid;
        private String mac;
        private Integer slaveId;
        private String model;
    }
}
