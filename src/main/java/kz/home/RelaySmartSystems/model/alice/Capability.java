package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.*;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Table(name = "alice_capabilities")
//@JsonIgnoreProperties(value = {"mqttTopic", "mqttStateTopic"}, allowSetters = true)
public class Capability {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    Long id;
    private String type;
    @JsonRawValue
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String state;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonRawValue
    private String parameters;
    @JsonBackReference
    @ManyToOne //(fetch = FetchType.LAZY)
    @JoinColumn(name="device_id")//, referencedColumnName = "id")
    private Device device;

    public Device getDevice() {
        return device;
    }
    public void setDevice(Device device) {
        this.device = device;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setState(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }

    public String getParameters() {
        return parameters;
    }
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }


    @Override
    public String toString() {
        return String.format("Capability[id=%d, type='%s', state='%s']", id, type, state);
    }

}
