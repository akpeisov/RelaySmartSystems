package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "rc_outputs")
@Setter
@Getter
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
    private Integer slaveId = 0;

    @JsonBackReference
    @ManyToOne(optional = false)  //(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    private RelayController relayController;

//    @Transient
//    private String outputID;
//
//    public String getOutputID() {
//        return String.format("s%do%d", this.slaveId, this.id);
//    }
}
