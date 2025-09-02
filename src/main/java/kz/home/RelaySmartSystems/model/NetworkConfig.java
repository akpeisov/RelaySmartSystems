package kz.home.RelaySmartSystems.model;

import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "network_config")
public class NetworkConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    @OneToOne
    @JoinColumn(name = "controller_uuid", unique = true)
    private RelayController controller;

    private String ntpServer;
    private String ntpTZ;
    private String otaURL;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cloud_id")
    private CloudConfig cloud;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "eth_id")
    private EthConfig eth;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "wifi_id")
    private WifiConfig wifi;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ftp_id")
    private FtpConfig ftp;
}
