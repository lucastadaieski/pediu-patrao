package com.umc.pediupatrao.service;

import static com.umc.pediupatrao.service.AuditoriaService.campo;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.UsuarioAtual;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    private static final int TAMANHO_MINIMO_SENHA = 6;
    private static final String ENTIDADE = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final UsuarioAtual usuarioAtual;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService, UsuarioAtual usuarioAtual) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.usuarioAtual = usuarioAtual;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Usuario criar(UsuarioRequest dados) {
        exigirUsername(dados);
        String username = dados.username().trim();
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RegraNegocioException("Já existe um usuário com o username '" + username + "'.");
        }
        validarSenha(dados.password(), true);
        exigirPerfil(dados);

        Usuario usuario = new Usuario(username, passwordEncoder.encode(dados.password()), dados.perfil());
        Usuario salvo = usuarioRepository.save(usuario);

        auditoriaService.registrar(TipoOperacao.CRIACAO, ENTIDADE, salvo.getId(),
                List.of(campo("username", null, salvo.getUsername()), campo("perfil", null, salvo.getRole())),
                null);
        return salvo;
    }

    public Usuario atualizar(String id, UsuarioRequest dados) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        exigirUsername(dados);
        exigirPerfil(dados);

        String novoUsername = dados.username().trim();
        usuarioRepository.findByUsername(novoUsername)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("Já existe um usuário com o username '" + novoUsername + "'.");
                });

        boolean ehOProprio = usuario.getUsername().equals(usuarioAtual.username());
        if (ehOProprio && dados.perfil() != usuario.getRole()) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
        }

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        if (!novoUsername.equals(usuario.getUsername())) {
            alteracoes.add(campo("username", usuario.getUsername(), novoUsername));
            usuario.setUsername(novoUsername);
        }
        if (dados.perfil() != usuario.getRole()) {
            alteracoes.add(campo("perfil", usuario.getRole(), dados.perfil()));
            usuario.setRole(dados.perfil());
        }
        // Só troca a senha se uma nova foi informada. A auditoria registra QUE
        // a senha foi redefinida, nunca o valor.
        if (dados.password() != null && !dados.password().isBlank()) {
            validarSenha(dados.password(), true);
            usuario.setPassword(passwordEncoder.encode(dados.password()));
            alteracoes.add(new AlteracaoCampo("redefinicaoSenha", null, "realizada"));
        }

        Usuario salvo = usuarioRepository.save(usuario);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO, ENTIDADE, id, alteracoes, null);
        }
        return salvo;
    }

    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (usuario.getUsername().equals(usuarioAtual.username())) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio usuário.");
        }
        usuarioRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO, ENTIDADE, id,
                List.of(campo("username", usuario.getUsername(), null), campo("perfil", usuario.getRole(), null)),
                null);
    }

    private void validarSenha(String senha, boolean obrigatoria) {
        if (senha == null || senha.isBlank()) {
            if (obrigatoria) {
                throw new RegraNegocioException("Informe a senha.");
            }
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("A senha deve ter ao menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    private void exigirUsername(UsuarioRequest dados) {
        if (dados.username() == null || dados.username().trim().length() < 3) {
            throw new RegraNegocioException("Informe um username com ao menos 3 caracteres.");
        }
    }

    private void exigirPerfil(UsuarioRequest dados) {
        if (dados.perfil() == null) {
            throw new RegraNegocioException("Informe o perfil (ADMIN, GERENTE ou ATENDENTE).");
        }
    }
}
