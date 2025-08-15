package kz.home.RelaySmartSystems.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class RCTaskDTO {
    private String name;
    private Integer grace;
    private Integer time;
    private boolean done;
    private boolean enabled;
    private Set<Integer> dow;
    private List<RCTaskActionDTO> actions;
}

