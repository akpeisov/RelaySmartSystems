package kz.home.RelaySmartSystems.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class RCUpdateIODTO {
    private List<RCState> outputs;
    private List<RCState> inputs;

    @Data
    public static class RCState {
        private Integer id;
        private String state;
        private Integer slaveId = 0;
    }
}
