package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.validator.constraints.UniqueElements;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "controllers")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
    @Transient
    private Object controllerData;

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
}
