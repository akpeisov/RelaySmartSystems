package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.alice.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import kz.home.RelaySmartSystems.model.*;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByUser(User user);
    Device save (Device device);
}
