package com.umc.pediupatrao.entity.enums;

public enum TipoAtendimento {

    ENTREGA("Entrega"),
    RETIRADA("Retirada no balcão");

    private final String descricao;

    TipoAtendimento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusPedido statusDeSaida() {
        return this == ENTREGA ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO;
    }
}
