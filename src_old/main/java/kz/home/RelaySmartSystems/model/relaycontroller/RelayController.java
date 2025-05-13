package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.*;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RelayController {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String mac;
    @JsonIgnore
    @ManyToOne
    private User user;
    // mappedBy - имя "колонки" (точнее поля) в дочерней таблице, по которой будет связка с id данной
    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private Set<RCOutput> outputs; // = new ArrayList<RCOutput>();

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private Set<RCInput> inputs;

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
    public Set<RCOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(Set<RCOutput> outputs) {
        this.outputs = outputs;
    }

    public Set<RCInput> getInputs() {
        return inputs;
    }

    public void setInputs(Set<RCInput> inputs) {
        this.inputs = inputs;
    }

    public User getUser() {
        return user;
    }
}
