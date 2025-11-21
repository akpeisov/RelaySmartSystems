package kz.home.RelaySmartSystems.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rc_ftp_config")
public class FtpConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private boolean enabled;
    private String user;
    private String pass;
}
