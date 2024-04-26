package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import kz.home.RelaySmartSystems.filters.PositiveIntegerFilter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_rules")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCRule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String event;
    private Integer output;
    private String action;
    private Long duration;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = PositiveIntegerFilter.class)
    private Integer slaveid;
    private String type;
    @JsonBackReference
    @JoinColumn(name = "input_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCInput input;
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL)
    private List<RCAction> actions;
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL)
    private List<RCAcl> acls;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getSlaveid() {
        return slaveid;
    }

    public void setSlaveid(Integer slaveid) {
        this.slaveid = slaveid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<RCAction> getActions() {
        return actions;
    }

    public void setActions(List<RCAction> actions) {
        this.actions = actions;
    }

    public List<RCAcl> getAcls() {
        return acls;
    }

    public void setAcls(List<RCAcl> acls) {
        this.acls = acls;
    }

    public RCInput getInput() {
        return input;
    }

    public void setInput(RCInput input) {
        this.input = input;
    }
}