package kz.home.RelaySmartSystems.model;

import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserDevices {
    String username;
    String userfio;
//    List<RelayController> relayControllers;
    List<Controller> controllers;
}
