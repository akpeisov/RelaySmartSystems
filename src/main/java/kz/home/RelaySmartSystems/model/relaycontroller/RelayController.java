package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kz.home.RelaySmartSystems.model.User;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Table(name = "rc_controllers")
public class RelayController {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String mac;
    @ManyToOne
    private User user;
    private String type;
    private String name;
    private String status;
    // mappedBy - имя "колонки" (точнее поля) в дочерней таблице, по которой будет связка с id данной
    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL)
    private List<Output> outputs = new ArrayList<Output>();

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL)
    private List<Input> inputs;

    @JsonIgnore
    public UUID getUuid() {
        return uuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Output> outputs) {
        this.outputs = outputs;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    public void setInputs(List<Input> inputs) {
        this.inputs = inputs;
    }
}
