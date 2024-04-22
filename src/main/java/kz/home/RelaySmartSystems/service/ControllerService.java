package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.Controller;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.ControllerRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ControllerService {
    private final ControllerRepository controllerRepository;

    public ControllerService(ControllerRepository controllerRepository) {
        this.controllerRepository = controllerRepository;
    }

    public boolean isControllerLinked(String mac) {
        // TODO : проверить по всем видам?
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        return !(c == null || c.getUser() == null);
    }

    public void addController(String mac, String type) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            if (c.getType().equals(type))
                return;
        } else {
            c = new Controller();
            c.setMac(mac);
        }
        c.setType(type);
        controllerRepository.save(c);
    }

    public String linkController(String mac, User user) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            c.setUser(user);
            c.setLinkDate(new Date());
            controllerRepository.save(c);
            return "OK";
        }
        return "NOT_FOUND";
    }

    public String unlinkController(String mac, User user) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            c.setUser(null);
            controllerRepository.save(c);
            return "OK";
        }
        return "NOT_FOUND";
    }

    public Controller findController(String mac) {
        return controllerRepository.findByMac(mac.toUpperCase());
    }

    public User getUserByController(String mac) {
        Controller controller = controllerRepository.findByMac(mac);
        if (controller != null)
            return controller.getUser();
        return null;
    }
}
