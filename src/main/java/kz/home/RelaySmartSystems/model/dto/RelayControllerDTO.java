package kz.home.RelaySmartSystems.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RelayControllerDTO {
    private UUID uuid;
    private String name;
    private String mac;
    private String type;
    private String status;
    private List<RCOutputDTO> outputs;
    private List<RCInputDTO> inputs;
}