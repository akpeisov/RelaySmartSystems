package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCInputDTO {
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    private String state;
    private Integer slaveId;
    private List<RCEventDTO> events;

    public RCInputDTO(UUID uuid, Integer id, String name, String type, String state, Integer slaveId,
                      List<RCEventDTO> events) {
        this.uuid = uuid;
        this.id = id;
        this.name = name;
        this.type = type;
        this.state = state;
        this.events = events;
        this.slaveId = slaveId;
    }
}
