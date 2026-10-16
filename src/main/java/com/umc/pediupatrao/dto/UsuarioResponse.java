package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.enums.Perfil;
import com.umc.pediupatrao.entity.Usuario;

public record UsuarioResponse(String id, String username, Perfil perfil) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getRole());
    }
}
