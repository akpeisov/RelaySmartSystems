package kz.home.RelaySmartSystems.model.relaycontroller;

import kz.home.RelaySmartSystems.model.Controller;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rc_controllers")
//@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude(JsonInclude.Include.NON_EMPTY)
//@DiscriminatorValue("RelayController")
public class RelayController extends Controller {
    // mappedBy - имя "колонки" (точнее поля) в дочерней таблице, по которой будет связка с id данной
    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<RCOutput> outputs;

    @OneToMany(mappedBy = "relayController", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<RCInput> inputs;

    @OneToOne(mappedBy = "controller", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RCModbusConfig modbusConfig;
}
