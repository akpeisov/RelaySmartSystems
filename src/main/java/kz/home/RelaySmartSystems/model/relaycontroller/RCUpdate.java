package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RCUpdate {
    private String mac;
    private String state;
    private Integer output;
    private Integer input;
    private Integer slaveId;
    private Integer timer;

    public String makeMessage() {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("type", "UPDATE");
        objectMap.put("payload", this);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(objectMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getOutput() {
        return output;
    }

    public void setOutput(Integer output) {
        this.output = output;
    }

    public Integer getInput() {
        return input;
    }

    public void setInput(Integer input) {
        this.input = input;
    }

    public Integer getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(Integer slaveId) {
        this.slaveId = slaveId;
    }

    public Integer getTimer() {
        return timer;
    }

    public void setTimer(Integer timer) {
        this.timer = timer;
    }
}
