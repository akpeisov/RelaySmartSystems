package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;

public class AliceCapability {
    private String type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private AliceState state;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String parameters;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AliceState getState() {
        return state;
    }

    public void setState(AliceState state) {
        this.state = state;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
