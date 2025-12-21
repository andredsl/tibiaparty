package com.tibia.app.dto.request;

import com.tibia.app.domain.enums.PartyType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartyRequest {

    @NotNull(message = "Tipo de PT e obrigatorio")
    private PartyType type;

    @Size(max = 100, message = "Local deve ter no maximo 100 caracteres")
    private String location; // Para Hunt

    @Size(max = 100, message = "Nome do boss deve ter no maximo 100 caracteres")
    private String bossName; // Para Boss

    @Size(max = 100, message = "Nome do Soul Core deve ter no maximo 100 caracteres")
    private String soulCoreName; // Para Soul Core

    @Min(value = 0, message = "Preco por player nao pode ser negativo")
    private Long pricePerPlayer; // Para Soul Core

    @Builder.Default
    private Boolean leaderParticipates = true; // Se o lider participa da PT ou apenas gerencia

    @Min(value = 8, message = "Level minimo deve ser pelo menos 8")
    @Max(value = 4000, message = "Level minimo nao pode exceder 4000")
    private Integer levelMin;

    @Min(value = 8, message = "Level maximo deve ser pelo menos 8")
    @Max(value = 4000, message = "Level maximo nao pode exceder 4000")
    private Integer levelMax;

    @Min(value = 2, message = "Minimo de 2 membros")
    @Max(value = 20, message = "Maximo de 20 membros")
    private Integer maxMembers;

    // Vagas por vocacao
    @Min(value = 0, message = "Quantidade de EK nao pode ser negativa")
    @Max(value = 10, message = "Maximo de 10 EKs")
    @Builder.Default
    private Integer slotsEK = 0;

    @Min(value = 0, message = "Quantidade de RP nao pode ser negativa")
    @Max(value = 10, message = "Maximo de 10 RPs")
    @Builder.Default
    private Integer slotsRP = 0;

    @Min(value = 0, message = "Quantidade de ED nao pode ser negativa")
    @Max(value = 10, message = "Maximo de 10 EDs")
    @Builder.Default
    private Integer slotsED = 0;

    @Min(value = 0, message = "Quantidade de MS nao pode ser negativa")
    @Max(value = 10, message = "Maximo de 10 MSs")
    @Builder.Default
    private Integer slotsMS = 0;

    private LocalDateTime scheduledTime; // Para Boss

    @Builder.Default
    private Boolean startWhenFull = false; // Iniciar quando a PT estiver completa

    @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
    private String description;

    public int getTotalVocationSlots() {
        return (slotsEK != null ? slotsEK : 0) +
               (slotsRP != null ? slotsRP : 0) +
               (slotsED != null ? slotsED : 0) +
               (slotsMS != null ? slotsMS : 0);
    }

    public boolean hasVocationRequirements() {
        return getTotalVocationSlots() > 0;
    }
}
