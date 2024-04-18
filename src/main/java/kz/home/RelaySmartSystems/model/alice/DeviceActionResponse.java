package kz.home.RelaySmartSystems.model.alice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class DeviceActionResponse {
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

        public List<Device> getDevices() {
            return devices;
        }

        public void setDevices(List<Device> devices) {
            this.devices = devices;
        }

        public static class Device {
            private String id;
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private List<Capability> capabilities;

            public ActionResult getAction_result() {
                return action_result;
            }

            public void setAction_result(ActionResult action_result) {
                this.action_result = action_result;
            }
            @JsonInclude(JsonInclude.Include.NON_NULL)
            private ActionResult action_result;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public List<Capability> getCapabilities() {
                return capabilities;
            }

            public void setCapabilities(List<Capability> capabilities) {
                this.capabilities = capabilities;
            }

            public static class Capability {
                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public State getState() {
                    return state;
                }

                public void setState(State state) {
                    this.state = state;
                }

                private String type;
                private State state;

                public static class State {
                    private String instance;

                    public String getInstance() {
                        return instance;
                    }

                    public void setInstance(String instance) {
                        this.instance = instance;
                    }

                    public ActionResult getAction_result() {
                        return action_result;
                    }

                    public void setAction_result(ActionResult action_result) {
                        this.action_result = action_result;
                    }

                    private ActionResult action_result;


                }
            }

            public static class ActionResult {
                private String status;
                @JsonInclude(JsonInclude.Include.NON_NULL)
                private String error_code;
                @JsonInclude(JsonInclude.Include.NON_NULL)
                private String error_message;

                public ActionResult(String status, String error_code, String error_message) {
                    this.status = status;
                    this.error_code = error_code;
                    this.error_message = error_message;
                }

                public String getStatus() {
                    return status;
                }

                public void setStatus(String status) {
                    this.status = status;
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


            }
        }
    }



}