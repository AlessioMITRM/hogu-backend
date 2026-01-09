package us.hogu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import us.hogu.controller.dto.request.CommissionSettingRequestDto;
import us.hogu.controller.dto.response.CommissionSettingResponseDto;
import us.hogu.model.CommissionSetting;
import us.hogu.model.enums.ServiceType;
import us.hogu.service.intefaces.CommissionService;

@RestController
@RequestMapping("/admin/commissioni")
@RequiredArgsConstructor
@Tag(name = "Commissioni", description = "Gestione delle commissioni per i servizi")
public class CommissionController {

    private final CommissionService commissionService;

    @Operation(summary = "Crea una nuova commissione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Commissione creata con successo",
                content = @Content(schema = @Schema(implementation = CommissionSettingResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Dati non validi")
    })
    @PostMapping
    public ResponseEntity<CommissionSettingResponseDto> createCommissionSetting(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dati per la creazione di una nuova commissione",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CommissionSettingRequestDto.class))
            )
            @org.springframework.web.bind.annotation.RequestBody CommissionSettingRequestDto requestDto) {
        CommissionSetting commissionSetting = convertToEntity(requestDto);
        CommissionSetting saved = commissionService.createCommissionSetting(commissionSetting);
        return ResponseEntity.ok(convertToDto(saved));
    }

    @Operation(summary = "Aggiorna una commissione esistente")
    @PutMapping("/{id}")
    public ResponseEntity<CommissionSettingResponseDto> updateCommissionSetting(
            @PathVariable Long id,
            @RequestBody CommissionSettingRequestDto requestDto) {
        CommissionSetting commissionSetting = convertToEntity(requestDto);
        CommissionSetting updated = commissionService.updateCommissionSetting(id, commissionSetting);
        return ResponseEntity.ok(convertToDto(updated));
    }

    @Operation(summary = "Disabilita una commissione")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disableCommissionSetting(@PathVariable Long id) {
        commissionService.disableCommissionSetting(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ottiene la commissione attiva per un tipo di servizio")
    @GetMapping("/{serviceType}")
    public ResponseEntity<CommissionSettingResponseDto> getCurrentCommissionSetting(
            @PathVariable ServiceType serviceType) {
        CommissionSetting setting = commissionService.getCurrentCommissionSetting(serviceType);
        return ResponseEntity.ok(convertToDto(setting));
    }

    private CommissionSetting convertToEntity(CommissionSettingRequestDto dto) {
        return CommissionSetting.builder()
                .serviceType(dto.getServiceType())
                .commissionRate(dto.getCommissionRate())
                .minCommissionAmount(dto.getMinCommissionAmount())
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveTo(dto.getEffectiveTo())
                .build();
    }

    private CommissionSettingResponseDto convertToDto(CommissionSetting entity) {
        return CommissionSettingResponseDto.builder()
                .id(entity.getId())
                .serviceType(entity.getServiceType())
                .commissionRate(entity.getCommissionRate())
                .minCommissionAmount(entity.getMinCommissionAmount())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .build();
    }
}
