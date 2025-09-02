package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Setter
@Getter
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

    public RCAcl(String type, Integer id, String io, String state) {
        this.type = type;
        this.id = id;
        this.io = io;
        this.state = state;
    }

    public String getCompareId() {
        return String.format("%d%s%s%s", id, type, io, state);
    }
}
