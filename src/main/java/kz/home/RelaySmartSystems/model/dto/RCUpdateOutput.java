package kz.home.RelaySmartSystems.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCUpdateOutput {
    private UUID uuid;
    private String mac;
    private Integer id;
    private String name;
    private Long timer;
    private String type;
    private Boolean alice;
    private String room;
    @JsonProperty("default")
    private String _default;
    private Integer on;
    private Integer off;
    private Long limit;
    private Integer slaveId;
}
