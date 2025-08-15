package kz.home.RelaySmartSystems.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RCTaskActionDTO {
    private String action;
    private Integer duration;
    private Integer output;
    private String type;
    private Integer input;
}