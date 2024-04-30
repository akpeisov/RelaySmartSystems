package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCUpdateOutput {
    private String mac;
    private Integer id;
    private String name;
    private Long duration;
    private String type;
    private Boolean alice;
    @JsonProperty("default")
    private String _default;
    private Integer on;
    private Integer off;

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getAlice() {
        return alice;
    }

    public void setAlice(Boolean alice) {
        this.alice = alice;
    }

    public String get_default() {
        return _default;
    }

    public void set_default(String _default) {
        this._default = _default;
    }

    public Integer getOn() {
        return on;
    }

    public void setOn(Integer on) {
        this.on = on;
    }

    public Integer getOff() {
        return off;
    }

    public void setOff(Integer off) {
        this.off = off;
    }
}
