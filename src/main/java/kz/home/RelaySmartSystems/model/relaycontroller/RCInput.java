package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "rc_inputs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RCInput {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;
    private Integer id;
    private String name;
    private String type;
    private String state;
    private Integer slaveId;
    @OneToMany(mappedBy = "input", cascade = CascadeType.ALL, fetch = FetchType.EAGER) // без cascade = CascadeType.ALL при вставке не добавляются rules. Но и не удаляются все... а CascadeType.REMOVE удаляет, но не добавляет, EAGER сразу грузит дочерние объекты из базы, без него получаем ошибку lazyload
    private Set<RCEvent> events;
    @JsonBackReference
    @JoinColumn(name = "relay_controller_uuid", nullable=false)
    @ManyToOne(optional = false)
    private RelayController relayController;
}
