package kz.home.RelaySmartSystems.model.mapper;

import kz.home.RelaySmartSystems.model.dto.*;
import kz.home.RelaySmartSystems.model.relaycontroller.*;
import kz.home.RelaySmartSystems.repository.RCModbusConfigRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
public class RelayControllerMapper {

    private final RCModbusConfigRepository modbusConfigRepository;

    public RelayControllerMapper(RCModbusConfigRepository modbusConfigRepository) {
        this.modbusConfigRepository = modbusConfigRepository;
    }

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
        dto.setDescription(controller.getDescription());
        dto.setModel(controller.getModel());

        List<RCOutputDTO> outputs = new ArrayList<>(controller.getOutputs().stream()
                .map(o -> new RCOutputDTO(o.getUuid(), o.getId(), o.getName(), o.getLimit(), o.getType(), o.get_default(), o.getState(), o.getAlice(), o.getRoom(), o.getOn(), o.getOff()))
                .toList());


        List<RCInputDTO> inputs = new ArrayList<>(controller.getInputs().stream()
                .map(this::mapInput)
                .toList());
/*
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
*/
        dto.setOutputs(outputs);
        dto.setInputs(inputs);

        // modbus
        RCModbusConfig config = modbusConfigRepository.findByController(controller).orElse(null);
        if (config != null) {
            RCModbusInfoDTO modbusDto = new RCModbusInfoDTO();
            if (config.getMode().equals(ModbusMode.master)) {
                //modbusDto.setActionOnSameSlave(config. );
                modbusDto.setMaxRetries(config.getMaxRetries());
                modbusDto.setPollingTime(config.getPollingTime());
                modbusDto.setReadTimeout(config.getReadTimeout());
                // add slaves
            } else {
                // slave
                modbusDto.setSlaveId(config.getSlaveId());
                modbusDto.setMaster(config.getMaster());
            }
            dto.setModbus(modbusDto);
        }
/*
        // modbus
        RCModbusConfig config = modbusConfigRepository.findByController(controller).orElse(null); // inject modbusConfigRepo
        RCModbusInfoDTO modbusDto = new RCModbusInfoDTO();

        if (controller.isMaster()) {
            modbusDto.setMode("master");
            if (config != null) {
                modbusDto.setPollingTime(config.getPollingTime());
                modbusDto.setReadTimeout(config.getReadTimeout());
                modbusDto.setMaxRetries(config.getMaxRetries());
            }

            List<RCModbusInfoDTO.SlaveDTO> slaveDTOs = controller.getSlaves().stream()
                    .map(rcModbus -> {
                        RCModbusInfoDTO.SlaveDTO s = new RCModbusInfoDTO.SlaveDTO();
                        s.setUuid(rcModbus.getSlave().getUuid());
                        s.setSlaveId(rcModbus.getSlaveId());
                        s.setModel(rcModbus.getSlave().getModel());
                        return s;
                    }).toList();
            modbusDto.setSlaves(slaveDTOs);

        } else if (controller.isSlave()) {
            modbusDto.setMode("slave");
            modbusDto.setSlaveId(controller.getSlaveOf().getSlaveId());
            modbusDto.setMaster(controller.getSlaveOf().getMaster().getUuid());
        }

        dto.setModbus(modbusDto);
*/
        return dto;
    }
}
