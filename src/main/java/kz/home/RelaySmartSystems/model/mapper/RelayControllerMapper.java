package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RCModbusConfigRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
public class RelayControllerMapper {

    private RCActionDTO mapAction(RCAction action) {
        return new RCActionDTO(
                action.getUuid(),
                action.getOrder(),
                action.getOutput(),
                action.getAction(),
                action.getDuration(),
                action.getSlaveId()
        );
    }

    private RCAclDTO mapAcl(RCAcl acl) {
        return new RCAclDTO(
                acl.getUuid(),
                acl.getType(),
                acl.getId(),
                acl.getIo(),
                acl.getState()
        );
    }

    private RCEventDTO mapEvent(RCEvent event) {
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

    private RCInputDTO mapInput(RCInput input) {
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
//    public List<RCInputDTO> mapInputs(Set<RCInput> inputs) {
//        return inputs.stream()
//                .map(this::mapInput)
//                .toList();
//    }
//    public RelayControllerDTO toDto(RelayController controller) {
//        RelayControllerDTO dto = new RelayControllerDTO();
//        dto.setUuid(controller.getUuid());
//        dto.setName(controller.getName());
//        dto.setMac(controller.getMac());
//        dto.setType(controller.getType());
//        dto.setStatus(controller.getStatus());
//        dto.setDescription(controller.getDescription());
//        dto.setModel(controller.getModel());
//
//        List<RCOutputDTO> outputs = new ArrayList<>(controller.getOutputs().stream()
//                .map(o -> new RCOutputDTO(o.getUuid(), o.getId(), o.getName(), o.getLimit(), o.getType(), o.get_default(), o.getState(), o.getAlice(), o.getRoom(), o.getOn(), o.getOff()))
//                .toList());
//
//
//        List<RCInputDTO> inputs = new ArrayList<>(controller.getInputs().stream()
//                .map(this::mapInput)
//                .toList());
//        dto.setOutputs(outputs);
//        dto.setInputs(inputs);
//
//        return dto;
//    }

    public List<RCOutputDTO> outputsToDTO(List<RCOutput> outputs) {
        return new ArrayList<>(outputs.stream()
                .map(o -> new RCOutputDTO(o.getUuid(), o.getId(), o.getName(), o.getLimit(), o.getType(), o.get_default(), o.getState(), o.getAlice(), o.getRoom(), o.getOn(), o.getOff()))
                .toList());
    }

    public List<RCInputDTO> inputsToDTO(List<RCInput> inputs) {
        return new ArrayList<>(inputs.stream()
                .map(this::mapInput)
                .toList());
    }

    public RCModbusConfigDTO modbusToDTO(RCModbusConfig modbusInfo) {
        if (modbusInfo == null)
            return null;
        RCModbusConfigDTO rcModbusConfigDTO = new RCModbusConfigDTO();
        if (modbusInfo.getMode().equals(ModbusMode.master)) {
            rcModbusConfigDTO.setMode("master");
            rcModbusConfigDTO.setMaxRetries(modbusInfo.getMaxRetries());
            rcModbusConfigDTO.setReadTimeout(modbusInfo.getReadTimeout());
            rcModbusConfigDTO.setPollingTime(modbusInfo.getPollingTime());
            rcModbusConfigDTO.setActionOnSameSlave(modbusInfo.getActionOnSameSlave());
        } else if (modbusInfo.getMode().equals(ModbusMode.slave)) {
            rcModbusConfigDTO.setMode("slave");
            rcModbusConfigDTO.setSlaveId(modbusInfo.getSlaveId());
            rcModbusConfigDTO.setMaster(modbusInfo.getMaster());
        }

        return rcModbusConfigDTO;
    }
}

