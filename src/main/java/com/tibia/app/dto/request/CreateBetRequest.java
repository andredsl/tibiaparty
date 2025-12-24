package com.tibia.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBetRequest {

    @NotBlank(message = "Selecione uma criatura")
    private String creatureName;

    @NotNull(message = "Informe a quantidade de Tibia Coins")
    @Min(value = 25, message = "Aposta minima e de 25 Tibia Coins")
    private Long amount;

    public boolean isValidAmount() {
        return amount != null && amount >= 25 && amount % 25 == 0;
    }
}
