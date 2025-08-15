package kz.home.RelaySmartSystems.model.relaycontroller;

import com.fasterxml.jackson.annotation.*;
import kz.home.RelaySmartSystems.model.Controller;
import kz.home.RelaySmartSystems.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rc_controllers")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
//@DiscriminatorValue("RelayController")
public class RelayController extends Controller {
    // mappedBy - имя "колонки" (точнее поля) в дочерней таблице, по которой будет связка с id данной
    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private Set<RCOutput> outputs;

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private Set<RCInput> inputs;

//    @OneToMany(mappedBy = "master", cascade = CascadeType.ALL)
//    private List<RCModbus> slaves;
//
//    @OneToOne(mappedBy = "slave")
//    private RCModbus slaveOf;
//
//    public boolean isMaster() {
//        return slaves != null && !slaves.isEmpty();
//    }
//
//    public boolean isSlave() {
//        return slaveOf != null;
//    }
}
