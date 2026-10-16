package com.umc.pediupatrao.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAtual {

    public static final String SISTEMA = "sistema";
    public static final String ANONIMO = "anonimo";
    private static final String PREFIXO_ROLE = "ROLE_";

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AnonymousAuthenticationToken) {
            return ANONIMO; // tentativa sem login (ex.: POST sem token CSRF)
        }
        auth = autenticacao();
        return auth == null ? SISTEMA : auth.getName();
    }

    public String perfil() {
        Authentication auth = autenticacao();
        if (auth == null) {
            return "SISTEMA";
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(PREFIXO_ROLE))
                .map(a -> a.substring(PREFIXO_ROLE.length()))
                .findFirst()
                .orElse("SEM_PERFIL");
    }

    private Authentication autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth;
    }
}
