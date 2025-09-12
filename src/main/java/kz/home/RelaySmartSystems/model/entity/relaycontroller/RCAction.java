package kz.home.RelaySmartSystems.model.entity.relaycontroller;

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
    @JoinColumn(name = "event_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RCEvent event;
}
