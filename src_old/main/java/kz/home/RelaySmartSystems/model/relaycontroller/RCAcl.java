package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_acls")
public class RCAcl {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    String type;
    Integer id;
    String io;
    String state;
    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "event_uuid", nullable=false)
    RCEvent event;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIo() {
        return io;
    }

    public void setIo(String io) {
        this.io = io;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public RCEvent getEvent() {
        return event;
    }

    public void setEvent(RCEvent event) {
        this.event = event;
    }
    public String getCompareId() {
        return String.format("%d%s%s%s", id, type, io, state);
    }
}
