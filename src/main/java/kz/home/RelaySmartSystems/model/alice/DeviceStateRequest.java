package kz.home.RelaySmartSystems.model.alice;

import java.util.List;

public class DeviceStateRequest {
    private List<DeviceInfo> devices;

    public List<DeviceInfo> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceInfo> devices) {
        this.devices = devices;
    }

    public static class DeviceInfo {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}