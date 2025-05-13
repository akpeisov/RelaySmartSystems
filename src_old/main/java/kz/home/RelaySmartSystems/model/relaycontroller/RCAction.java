package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_actions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCAction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    @NotNull
    private Integer order;
    private Integer output;
    private String action;
    private Integer duration;
    private Integer slaveId;
    @JsonBackReference
    @JoinColumn(name = "event_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCEvent event;

    @Transient
    private String outputID;

    public String getOutputID() {
        return String.format("s%do%d", this.slaveId, this.output);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public RCEvent getEvent() {
        return event;
    }

    public void setEvent(RCEvent event) {
        this.event = event;
    }

    public Integer getOutput() {
        return output;
    }

    public void setOutput(Integer output) {
        this.output = output;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
    }

//    public String getCompareId() {
//        return String.format("%s%d%s%d", event, output, action, slaveId);
//    }
}
