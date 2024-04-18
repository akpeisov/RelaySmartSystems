package kz.home.RelaySmartSystems.model.alice;

import java.util.List;

public class DeviceActionRequest {
    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    private Payload payload;

    public static class Payload {
        private List<DeviceInfo> devices;

        public List<DeviceInfo> getDevices() {
            return devices;
        }

        public void setDevices(List<DeviceInfo> devices) {
            this.devices = devices;
        }

        public static class DeviceInfo {
            private String id;

            public List<Capability> getCapabilities() {
                return capabilities;
            }

            public void setCapabilities(List<Capability> capabilities) {
                this.capabilities = capabilities;
            }

            private List<Capability> capabilities;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public static class Capability {
                private String type;
                private State state;

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

                public static class State {
                    private String instance;
                    private Object value;

                    @Override
                    public String toString() {
                        return "{\"instance\": \"" + instance + "\"" +
                               ", \"value\": " + value + "}";
                    }

                    public String getInstance() {
                        return instance;
                    }

                    public void setInstance(String instance) {
                        this.instance = instance;
                    }

                    public Object getValue() {
                        return value;
                    }

                    public void setValue(Object value) {
                        this.value = value;
                    }

                    public boolean getBoolValue() {
                        return (boolean)value;
                    }
                }
            }
        }
    }
}


