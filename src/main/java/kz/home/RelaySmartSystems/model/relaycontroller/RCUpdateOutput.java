package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCUpdateOutput {
    private String mac;
    private Integer id;
    private String name;
    private Long timer;
    private String type;
    private Boolean alice;
    @JsonProperty("default")
    private String _default;
    private Integer on;
    private Integer off;
    private Integer slaveId;
}
