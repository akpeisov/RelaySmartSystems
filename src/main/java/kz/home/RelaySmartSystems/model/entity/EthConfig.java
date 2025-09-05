package kz.home.RelaySmartSystems.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "eth_config")
public class EthConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private boolean enabled;
    private boolean dhcp = true;
    private String ip;
    private String netmask;
    private String gateway;
    private String dns;
    private boolean enableReset;
    private Integer resetGPIO;
}
