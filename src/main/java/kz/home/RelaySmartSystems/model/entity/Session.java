package kz.home.RelaySmartSystems.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String sessionId;
    private Date startDate;
    private Date endDate;
    private Date lastActiveDate;
    private String remoteAddress;
    private String type;
    private String username;
    private String mac;
}
