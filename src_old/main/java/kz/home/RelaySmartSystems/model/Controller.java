package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "controllers")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EntityListeners(AuditingEntityListener.class)
public class Controller {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String mac;
    @JsonIgnore
    @ManyToOne
    private User user;
    private Date firstDate;
    private String type;
    private Date linkDate;
    private String name;
    private String status;
    private Integer uptime;
    private Integer freeMemory;
    private String version;
    private String ethip;
    private String wifiip;
    private String description;
    private Integer wifirssi;
    private Integer configTime;
    @LastModifiedDate
    private Date lastSeen;
    @Transient
    private Object controllerData;

    @Transient
    private boolean linked;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getFirstDate() {
        return firstDate;
    }

    public void setFirstDate(Date firstDate) {
        this.firstDate = firstDate;
    }

    @PrePersist
    void firstDate() {
        this.firstDate = new Date();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getLinkDate() {
        return linkDate;
    }

    public void setLinkDate(Date linkDate) {
        this.linkDate = linkDate;
    }

    public Object getControllerData() {
        return controllerData;
    }

    public void setControllerData(Object controllerData) {
        this.controllerData = controllerData;
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

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isLinked() {
        return user != null;
    }

    public Integer getConfigTime() {
        return configTime;
    }

    public void setConfigTime(Integer configTime) {
        this.configTime = configTime;
    }
}
