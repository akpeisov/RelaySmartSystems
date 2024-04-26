package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_outputs")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCOutput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private Long duration;
    private Long timer;
    private String type;
    @JsonProperty("default")
    @Column(name = "default")
    private String _default;
    private String state;

    private Boolean alice;
    private String room;

    @JsonBackReference
    @ManyToOne(optional = false)  //(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    private RelayController relayController;

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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
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
}
