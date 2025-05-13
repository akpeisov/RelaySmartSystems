package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_events")
@JsonIgnoreProperties(ignoreUnknown = true)

public class RCEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String event;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("order ASC")
    private Set<RCAction> actions;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<RCAcl> acls;

    @JsonBackReference
    @JoinColumn(name = "input_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCInput input;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public RCInput getInput() {
        return input;
    }

    public void setInput(RCInput input) {
        this.input = input;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Set<RCAction> getActions() {
        return actions;
    }

    public void setActions(Set<RCAction> actions) {
        this.actions = actions;
    }

    public Set<RCAcl> getAcls() {
        return acls;
    }

    public void setAcls(Set<RCAcl> acls) {
        this.acls = acls;
    }
}
