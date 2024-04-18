package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class DeviceStateResponse {
    private String request_id;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    private Payload payload;

    public static class Payload {

        private List<Device> devices;

        // Геттеры и сеттеры

        public List<Device> getDevices() {
            return devices;
        }

        public void setDevices(List<Device> devices) {
            this.devices = devices;
        }

        public static class Device {
            private String id;
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private String error_code; // Дополнительное поле

            @JsonInclude(JsonInclude.Include.NON_NULL)
            private String error_message; // Дополнительное поле
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private List<Capability> capabilities;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getError_code() {
                return error_code;
            }

            public void setError_code(String error_code) {
                this.error_code = error_code;
            }

            public String getError_message() {
                return error_message;
            }

            public void setError_message(String error_message) {
                this.error_message = error_message;
            }

            public List<Capability> getCapabilities() {
                return capabilities;
            }

            public void setCapabilities(List<Capability> capabilities) {
                this.capabilities = capabilities;
            }
        }
    }
}
