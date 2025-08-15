package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCActionDTO {
    private UUID uuid;
    private Integer order;
    private Integer output;
    private String action;
    private Integer duration; // только для action = wait
    private Integer slaveId;
    private String outputID;
    public Integer getSlaveId() {
        return slaveId == null ? 0 : slaveId;
    }
    public String getOutputID() {
        return String.format("s%do%d", getSlaveId(), this.output);
    }

    public RCActionDTO(UUID uuid, Integer order, Integer output, String action, Integer duration, Integer slaveId) {
        this.uuid = uuid;
        this.order = order;
        this.output = output;
        this.action = action;
        this.duration = duration;
        this.slaveId = slaveId;
    }
}
