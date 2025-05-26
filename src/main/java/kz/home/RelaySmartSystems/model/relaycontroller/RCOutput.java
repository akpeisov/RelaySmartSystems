package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.*;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rc_outputs")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
//@JsonPropertyOrder({"id"}) // какое свойство в каком порядке будет внутри объекта, это не сортировка
public class RCOutput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private Long limit;
    private Long timer;
    private String type;
    @JsonProperty("default")
    @Column(name = "default")
    private String _default;
    private String state;
    private Boolean alice;
    private String room;
    private Integer on;
    private Integer off;
//    @Column(name="slave_id" default 0)
    private Integer slaveId = 0; // don't work, always null

    @JsonBackReference
    @ManyToOne(optional = false)  //(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    private RelayController relayController;

    @Transient
    private String outputID;

    public String getOutputID() {
        return String.format("s%do%d", this.slaveId, this.id);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public Long getTimer() {
        return timer;
    }

    public void setTimer(Long timer) {
        this.timer = timer;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String get_default() {
        return _default;
    }

    public void set_default(String _default) {
        this._default = _default;
    }

    public RelayController getRelayController() {
        return relayController;
    }
    public void setRelayController(RelayController relayController) {
        this.relayController = relayController;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getAlice() {
        return alice;
    }

    public void setAlice(Boolean alice) {
        this.alice = alice;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
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

    public Integer getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
    }
}
