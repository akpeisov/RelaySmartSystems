package kz.home.RelaySmartSystems.model.entity.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Setter
@Getter
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
    private Integer duration; // только для action = wait
    private Integer slaveId;
    @JsonBackReference
    @JoinColumn(name = "event_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCEvent event;

//    @Transient
//    private String outputID;

    public String getOutputID() {
        return String.format("s%do%d", this.slaveId, this.output);
    }

    public RCAction(Integer order, Integer output, String action, Integer duration, Integer slaveId) {
        this.order = order;
        this.output = output;
        this.action = action;
        this.duration = duration;
        this.slaveId = slaveId;
    }

    //    public String getCompareId() {
//        return String.format("%s%d%s%d", event, output, action, slaveId);
//    }
}
