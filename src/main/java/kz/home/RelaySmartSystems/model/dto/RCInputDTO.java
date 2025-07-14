package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RCInputDTO {
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    private String state;
    private Integer slaveId;
    private Set<RCEventDTO> events;

    public RCInputDTO(UUID uuid, Integer id, String name, String type, String state, Set<RCEventDTO> events) {
        this.uuid = uuid;
        this.id = id;
        this.name = name;
        this.type = type;
        this.state = state;
        this.events = events;
    }

    public RCInputDTO(UUID uuid, Integer id, String name, Integer slaveId) {
        this.uuid = uuid;
        this.id = id;
        this.name = name;
        this.slaveId = slaveId;
    }
}
