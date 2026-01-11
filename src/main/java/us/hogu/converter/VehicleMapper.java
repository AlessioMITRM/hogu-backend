package us.hogu.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import us.hogu.controller.dto.request.VehicleRequestDto;
import us.hogu.controller.dto.response.VehicleResponseDto;
import us.hogu.model.VehicleEntity;

@RequiredArgsConstructor
@Component
public class VehicleMapper {

	
    public VehicleResponseDto toDto(VehicleEntity entity) {
        if (entity == null) return null;

        return VehicleResponseDto.builder()
                .id(entity.getId())
                .plateNumber(entity.getPlateNumber())
                .model(entity.getModel())
                .type(entity.getType())
                .build();
    }

    public VehicleEntity toEntity(VehicleRequestDto dto) {
        if (dto == null) return null;

        return VehicleEntity.builder()
                .id(dto.getId())
                .numberOfSeats(dto.getNumberOfSeats())
                .plateNumber(dto.getPlateNumber())
                .model(dto.getModel())
                .type(dto.getType())
                .build();
    }
    
    public List<VehicleResponseDto> toDtoList(List<VehicleEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                       .map(this::toDto)
                       .collect(Collectors.toList());
    }

    public List<VehicleEntity> toEntityList(List<VehicleRequestDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream()
                   .map(this::toEntity)
                   .collect(Collectors.toList());
    }
	
}
