package com.tibia.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequest {

    @NotBlank(message = "Código é obrigatório")
    @Pattern(regexp = "TPF-[RLC][A-Z2-9]{5}", message = "Formato de código inválido")
    private String code;
}
