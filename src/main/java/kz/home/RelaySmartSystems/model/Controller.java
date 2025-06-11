package kz.home.RelaySmartSystems.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "controllers")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
public class Controller {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private String mac;
    @JsonIgnore
    @ManyToOne
    private User user;
    private Date firstDate;
    private String type;
    private Date linkDate;
    private String name;
    private String description;
    private String status;
    private Integer uptimeRaw;
    private String uptime;
    private Integer freeMemory;
    private String version;
    private String ethIP;
    private String wifiIP;
    private Integer wifiRSSI;
    private long configTime;
    @LastModifiedDate
    private Date lastSeen;
//    @Transient
//    private Object controllerData;

    @Transient
    private boolean linked;

    @PrePersist
    void firstDate() {
        this.firstDate = new Date();
    }

    public boolean isLinked() {
        return user != null;
    }
}
