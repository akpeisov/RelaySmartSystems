package kz.home.RelaySmartSystems.model.def;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Action {
    private String mac;
    private Integer output;
    private Integer input;
    private String action;
    private Integer slaveId;
    private Integer duration;
}
