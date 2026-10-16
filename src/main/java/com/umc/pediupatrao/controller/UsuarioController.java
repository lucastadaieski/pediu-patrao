package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioResponse;
import com.umc.pediupatrao.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de usuários (somente ADMIN).
 * ANTES: rota liberada sem login e recebia/devolvia a entidade com a senha.
 * AGORA: exige ADMIN, recebe UsuarioRequest e devolve UsuarioResponse (sem senha).
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listarTodos() {
        return usuarioService.listarTodos().stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable String id) {
        return usuarioService.buscarPorId(id)
                .map(u -> ResponseEntity.ok(UsuarioResponse.de(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criarUsuario(@Valid @RequestBody UsuarioRequest dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuarioService.criar(dados)));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizarUsuario(@PathVariable String id, @Valid @RequestBody UsuarioRequest dados) {
        return UsuarioResponse.de(usuarioService.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id) {
        usuarioService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
