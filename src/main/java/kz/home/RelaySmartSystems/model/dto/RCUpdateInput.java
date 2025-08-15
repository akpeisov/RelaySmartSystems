package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import kz.home.RelaySmartSystems.model.relaycontroller.RCEvent;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCUpdateInput {
    private UUID uuid;
    private String mac;
    private Integer id;
    private String name;
    private String type;
    private List<RCEvent> events;
}
