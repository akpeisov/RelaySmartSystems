package kz.home.RelaySmartSystems.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "wifi_config")
public class WifiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private boolean enabled;
    private boolean dhcp;
    private String ip;
    private String netmask;
    private String gateway;
    private String ssid;
    private String pass;
}