package kz.home.RelaySmartSystems.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RCEventDTO {
    private UUID uuid;
    private String event;
    private Set<RCActionDTO> actions;
    private Set<RCAclDTO> acls;
}
