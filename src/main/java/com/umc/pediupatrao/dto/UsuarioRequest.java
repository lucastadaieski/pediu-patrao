package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.enums.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "Informe o username")
        @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres")
        String username,
        String password,
        @NotNull(message = "Informe o perfil") Perfil perfil) {
}
