package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import kz.home.RelaySmartSystems.model.User;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_controllers")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RelayController {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String mac;
    @JsonIgnore
    @ManyToOne
    private User user;
    private String type;
    private String name;
    private String status;
    private Integer uptime;
    private Integer freeMemory;
    private String version;
    private String ethip;
    private String wifiip;
    private String description;
    private Integer wifirssi;
    // mappedBy - имя "колонки" (точнее поля) в дочерней таблице, по которой будет связка с id данной
    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL)
    private List<RCOutput> outputs = new ArrayList<RCOutput>();

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL)
    private List<RCInput> inputs;

    @JsonIgnore
    public UUID getUuid() {
        return uuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<RCOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<RCOutput> outputs) {
        this.outputs = outputs;
    }

    public List<RCInput> getInputs() {
        return inputs;
    }

    public void setInputs(List<RCInput> inputs) {
        this.inputs = inputs;
    }

    public Integer getUptime() {
        return uptime;
    }

    public void setUptime(Integer uptime) {
        this.uptime = uptime;
    }

    public Integer getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(Integer freeMemory) {
        this.freeMemory = freeMemory;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEthip() {
        return ethip;
    }

    public void setEthip(String ethip) {
        this.ethip = ethip;
    }

    public String getWifiip() {
        return wifiip;
    }

    public void setWifiip(String wifiip) {
        this.wifiip = wifiip;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getWifirssi() {
        return wifirssi;
    }

    public void setWifirssi(Integer wifirssi) {
        this.wifirssi = wifirssi;
    }

    public User getUser() {
        return user;
    }
}
