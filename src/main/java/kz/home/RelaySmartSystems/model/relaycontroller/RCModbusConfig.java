package kz.home.RelaySmartSystems.model.relaycontroller;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rc_modbus_config")
public class RCModbusConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    @OneToOne
    @JoinColumn(name = "controller_uuid", unique = true)
    private RelayController controller; // ссылка на контроллер, не важно мастер это или слейв

    @Enumerated(EnumType.STRING)
    private ModbusMode mode;

    // Только для мастера
    private Integer pollingTime;
    private Integer readTimeout;
    private Integer maxRetries;
    private Boolean actionOnSameSlave;

    // Только для слейва
    private Integer slaveId;
    private String master;
    private UUID masterUUID;
}