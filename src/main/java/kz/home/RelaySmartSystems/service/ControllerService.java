package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.entity.Controller;
import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.model.def.Info;
import kz.home.RelaySmartSystems.repository.ControllerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ControllerService {
    private final ControllerRepository controllerRepository;
    private static final Logger logger = LoggerFactory.getLogger(ControllerService.class);
    public ControllerService(ControllerRepository controllerRepository) {
        this.controllerRepository = controllerRepository;
    }

    public boolean isControllerLinked(String mac) {
        // TODO : проверить по всем видам?
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        return !(c == null || c.getUser() == null);
    }

    public User findControllerOwner(String mac) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        return c.getUser();
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

    public List<Controller> getUserControllers(User user) {
        return controllerRepository.findByUser(user);
    }

    public void setControllerInfo(Info info) {
        Controller c = controllerRepository.findByMac(info.getMac().toUpperCase());
        if (c != null) {
            try {
                c.setUptime(info.getUptime());
                c.setUptimeRaw(info.getUptimeRaw());
                c.setFreeMemory(info.getFreeMemory());
                c.setVersion(info.getVersion());
                c.setEthIP(info.getEthIP());
                c.setWifiIP(info.getWifiIP());
                c.setName(info.getName());
                c.setDescription(info.getDescription());
                c.setWifiRSSI(info.getWifiRSSI());
                c.setModel(info.getModel());
                c.setStatus("online");
                controllerRepository.save(c);
            }
            catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        }
    }

    private void setControllerStatus(String mac, String status) {
        Controller c = controllerRepository.findByMac(mac.toUpperCase());
        if (c != null) {
            c.setStatus(status);
            controllerRepository.save(c);
        }
    }

    public void setControllerOnline(String mac) {
        setControllerStatus(mac, "online");
    }

    public void setControllerOffline(String mac) {
        setControllerStatus(mac, "offline");
    }

    public void setOffline() {
        // проверить контроллеры, которые могли отвалиться и выставить им offline
        controllerRepository.setOffline();
    }

    public boolean isControllerBelongs(UUID uuid, User user) {
        Optional<Controller> c = controllerRepository.findById(uuid);
        return c.map(controller -> controller.getUser().equals(user)).orElse(false);
    }
}
