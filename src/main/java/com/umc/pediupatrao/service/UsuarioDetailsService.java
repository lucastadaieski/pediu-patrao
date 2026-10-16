package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDetailsService.class);

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        User.UserBuilder builder = User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword());

        if (usuario.getRole() == null) {
            log.warn("Usuário '{}' sem perfil definido: login bloqueado", usuario.getUsername());
            return builder.authorities(List.of()).disabled(true).build();
        }

        // roles("GERENTE") gera a authority "ROLE_GERENTE", usada por hasRole('GERENTE')
        return builder.roles(usuario.getRole().name()).build();
    }
}
