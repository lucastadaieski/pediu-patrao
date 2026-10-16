package com.umc.pediupatrao.entity.enums;

public enum TipoOperacao {

    CRIACAO("Inclusão"),
    ALTERACAO("Alteração"),
    EXCLUSAO("Exclusão"),
    MUDANCA_STATUS("Mudança de status"),
    DESCONTO("Aplicação de desconto"),
    CANCELAMENTO("Cancelamento"),
    ACESSO_NEGADO("Tentativa de acesso negada");

    private final String descricao;

    TipoOperacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
