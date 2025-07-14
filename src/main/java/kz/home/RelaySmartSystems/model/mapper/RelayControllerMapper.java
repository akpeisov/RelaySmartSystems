package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelayControllerMapper {

    public RCActionDTO mapAction(RCAction action) {
        return new RCActionDTO(
                action.getUuid(),
                action.getOrder(),
                action.getOutput(),
                action.getAction(),
                action.getDuration(),
                action.getSlaveId()
        );
    }

    public RCAclDTO mapAcl(RCAcl acl) {
        return new RCAclDTO(
                acl.getUuid(),
                acl.getType(),
                acl.getId(),
                acl.getIo(),
                acl.getState()
        );
    }
    public RCEventDTO mapEvent(RCEvent event) {
        Set<RCActionDTO> actions = event.getActions().stream()
                .map(this::mapAction)
                .collect(Collectors.toSet());

        Set<RCAclDTO> acls = event.getAcls().stream()
                .map(this::mapAcl)
                .collect(Collectors.toSet());

        return new RCEventDTO(
                event.getUuid(),
                event.getEvent(),
                actions,
                acls
        );
    }
    public RCInputDTO mapInput(RCInput input) {
        return new RCInputDTO(
                input.getUuid(),
                input.getId(),
                input.getName(),
                input.getType(),
                input.getState(),
                input.getEvents().stream()
                        .map(this::mapEvent)
                        .collect(Collectors.toSet())
        );
    }
    public List<RCInputDTO> mapInputs(Set<RCInput> inputs) {
        return inputs.stream()
                .map(this::mapInput)
                .toList();
    }
    public RelayControllerDTO toDto(RelayController controller) {
        RelayControllerDTO dto = new RelayControllerDTO();
        dto.setUuid(controller.getUuid());
        dto.setName(controller.getName());
        dto.setMac(controller.getMac());
        dto.setType(controller.getType());
        dto.setStatus(controller.getStatus());

        //List<RCInputDTO> inputs = new ArrayList<>();

        List<RCOutputDTO> outputs = new ArrayList<>(controller.getOutputs().stream()
                .map(o -> new RCOutputDTO(o.getUuid(), o.getId(), o.getName(), o.getLimit(), o.getType(), o.get_default(), o.getState(), o.getAlice(), o.getRoom(), o.getOn(), o.getOff()))
                .toList());

        //List<RCInputDTO> inputDTOs = mapInputs(controller.getInputs());
//        inputs.addAll(controller.getInputs().stream()
//                .map(i -> new RCInputDTO(i.getUuid(), i.getId(), i.getName(), i.getType(), i.getState(), mapEvent(i.getEvents())))
//                .toList());
        List<RCInputDTO> inputs = new ArrayList<>(controller.getInputs().stream()
                .map(this::mapInput) // <--- тот mapInput, что ты уже написал
                .toList());

        // Если мастер — добавим слейвов
        if (controller.isMaster()) {
            for (RCModbus modbus : controller.getSlaves()) {
                RelayController slave = modbus.getSlave();
                Integer slaveId = modbus.getSlaveId();
                outputs.addAll(slave.getOutputs().stream()
                        .map(o -> new RCOutputDTO(o.getUuid(), o.getId(), o.getName(), slaveId))
                        .toList());

                inputs.addAll(slave.getInputs().stream().filter(i -> i.getId() < 16) // only inputs without buttons
                        .map(i -> new RCInputDTO(i.getUuid(), i.getId(), i.getName(), slaveId))
                        .toList());
            }
        }

        dto.setOutputs(outputs);
        dto.setInputs(inputs);
        return dto;
    }
}
