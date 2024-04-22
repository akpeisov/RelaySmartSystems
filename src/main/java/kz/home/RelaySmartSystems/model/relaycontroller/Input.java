package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_inputs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Input {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    @OneToMany(mappedBy = "input", cascade = CascadeType.ALL) // без cascade = CascadeType.ALL при вставке не добавляются rules
    private List<Rule> rules;
    @JsonBackReference
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RelayController relayController;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public RelayController getRelayController() {
        return relayController;
    }
    public void setRelayController(RelayController relayController) {
        this.relayController = relayController;
    }
}
