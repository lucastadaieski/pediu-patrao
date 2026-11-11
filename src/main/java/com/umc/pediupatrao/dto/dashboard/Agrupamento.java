package com.umc.pediupatrao.dto.dashboard;

/** Como a série temporal de vendas é agrupada (RF02). */
public enum Agrupamento {

    DIA("Por dia"),
    SEMANA("Por semana"),
    MES("Por mês");

    private final String descricao;

    Agrupamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
