package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.umc.pediupatrao.entity.enums.TipoOperacao;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id; // gerado pelo MongoDB na inserção

    private final LocalDateTime dataHora;
    private final String usuario;
    private final String perfil;
    private final TipoOperacao operacao;
    private final String entidade;
    private final String entidadeId;
    private final List<AlteracaoCampo> alteracoes;
    private final String justificativa;

    @PersistenceCreator
    public RegistroAuditoria(String id, LocalDateTime dataHora, String usuario, String perfil,
                             TipoOperacao operacao, String entidade, String entidadeId,
                             List<AlteracaoCampo> alteracoes, String justificativa) {
        this.id = id;
        this.dataHora = dataHora;
        this.usuario = usuario;
        this.perfil = perfil;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes == null ? List.of() : List.copyOf(alteracoes);
        this.justificativa = justificativa;
    }


    public static RegistroAuditoria novo(LocalDateTime dataHora, String usuario, String perfil,
                                         TipoOperacao operacao, String entidade, String entidadeId,
                                         List<AlteracaoCampo> alteracoes, String justificativa) {
        return new RegistroAuditoria(null, dataHora, usuario, perfil, operacao, entidade,
                entidadeId, alteracoes, justificativa);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public TipoOperacao getOperacao() {
        return operacao;
    }


    @Transient // calculado: aparece no JSON, mas não é gravado no MongoDB
    public String getOperacaoDescricao() {
        return operacao == null ? null : operacao.getDescricao();
    }

    public String getEntidade() {
        return entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public List<AlteracaoCampo> getAlteracoes() {
        return alteracoes;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
