package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class AlicePayload {
    private List<AliceDevice> devices;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userId;

    public List<AliceDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<AliceDevice> devices) {
        this.devices = devices;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
