package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RCModbusConfigDTO {
    private String mode; // master | slave
    private String mac; // for controller identity

    // only for master
    private Integer pollingTime;
    private Integer readTimeout;
    private Integer maxRetries;
    private Boolean actionOnSameSlave;
    private List<SlaveDTO> slaves;

    // only for slave
    private Integer slaveId;
    private String master;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlaveDTO {
        private String mac;
        private Integer slaveId;
        private String model;
    }
}
