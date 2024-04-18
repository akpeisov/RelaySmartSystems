package kz.home.RelaySmartSystems.model;

import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;

import java.util.List;

public class UserDevices {
    String username;
    String userfio;
    List<RelayController> relayControllers;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserfio() {
        return userfio;
    }

    public void setUserfio(String userfio) {
        this.userfio = userfio;
    }
    public List<RelayController> getRelayControllers() {
        return relayControllers;
    }

    public void setRelayControllers(List<RelayController> relayControllers) {
        this.relayControllers = relayControllers;
    }
}
