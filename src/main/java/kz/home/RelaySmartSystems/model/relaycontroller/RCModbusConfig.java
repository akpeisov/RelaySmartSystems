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
    private RelayController controller;

    @Enumerated(EnumType.STRING)
    private ModbusMode mode;

    // Только для мастера
    private Integer pollingTime;
    private Integer readTimeout;
    private Integer maxRetries;

    // Только для слейва
    private Integer slaveId;
    private UUID master;
}