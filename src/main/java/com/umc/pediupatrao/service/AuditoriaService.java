package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioAtual;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private static final Set<String> CAMPOS_SENSIVEIS = Set.of("password", "senha", "token", "credencial");

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioAtual usuarioAtual;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioAtual usuarioAtual) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public RegistroAuditoria registrar(TipoOperacao operacao, String entidade, String entidadeId,
                                       List<AlteracaoCampo> alteracoes, String justificativa) {
        List<AlteracaoCampo> seguras = alteracoes == null ? List.of() : alteracoes.stream()
                .filter(a -> a != null && !CAMPOS_SENSIVEIS.contains(a.campo().toLowerCase()))
                .toList();

        RegistroAuditoria registro = RegistroAuditoria.novo(LocalDateTime.now(),
                usuarioAtual.username(), usuarioAtual.perfil(), operacao, entidade, entidadeId,
                seguras, justificativa);
        return auditoriaRepository.insert(registro);
    }

    public void registrarAcessoNegado(String rota, String motivo) {
        registrar(TipoOperacao.ACESSO_NEGADO, "Rota", rota, List.of(), motivo);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<RegistroAuditoria> consultar(String entidade, String entidadeId, String usuario,
                                             TipoOperacao operacao) {
        if (preenchido(entidade) && preenchido(entidadeId)) {
            return auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade, entidadeId.trim());
        }
        if (preenchido(entidade)) {
            return auditoriaRepository.findTop300ByEntidadeOrderByDataHoraDesc(entidade);
        }
        if (preenchido(usuario)) {
            return auditoriaRepository.findTop300ByUsuarioOrderByDataHoraDesc(usuario.trim());
        }
        if (operacao != null) {
            return auditoriaRepository.findTop300ByOperacaoOrderByDataHoraDesc(operacao);
        }
        return auditoriaRepository.findTop300ByOrderByDataHoraDesc();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<RegistroAuditoria> historico(String entidade, String entidadeId) {
        return auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade, entidadeId);
    }

    public static List<AlteracaoCampo> compararCampos(Map<String, Object> antes, Map<String, Object> depois) {
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        for (Map.Entry<String, Object> entrada : depois.entrySet()) {
            String campo = entrada.getKey();
            String valorAnterior = texto(antes.get(campo));
            String valorNovo = texto(entrada.getValue());
            if (!Objects.equals(valorAnterior, valorNovo)) {
                alteracoes.add(new AlteracaoCampo(campo, valorAnterior, valorNovo));
            }
        }
        return alteracoes;
    }


    public static AlteracaoCampo campo(String campo, Object anterior, Object novo) {
        return new AlteracaoCampo(campo, texto(anterior), texto(novo));
    }

    public static Map<String, Object> retrato(Object... paresCampoValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < paresCampoValor.length; i += 2) {
            mapa.put((String) paresCampoValor[i], paresCampoValor[i + 1]);
        }
        return mapa;
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal numero) {
            return numero.stripTrailingZeros().toPlainString();
        }
        String s = valor.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
