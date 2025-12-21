package com.tibia.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordLoginRequest {

    @NotBlank(message = "Nome do personagem é obrigatório")
    private String characterName;

    @NotBlank(message = "Senha é obrigatória")
    private String password;
}
