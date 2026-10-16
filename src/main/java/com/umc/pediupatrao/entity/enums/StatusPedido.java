package com.umc.pediupatrao.entity.enums;

import java.util.EnumSet;
import java.util.Set;


public enum StatusPedido {

    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Tabela de transições permitidas a partir deste status. */
    public Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case RECEBIDO -> EnumSet.of(EM_PREPARACAO, CANCELADO);
            case EM_PREPARACAO -> EnumSet.of(PRONTO, CANCELADO);
            case PRONTO -> EnumSet.of(SAIU_PARA_ENTREGA, RETIRADO, CANCELADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> EnumSet.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> EnumSet.noneOf(StatusPedido.class);
        };
    }

    public boolean podeIrPara(StatusPedido novo) {
        return novo != null && proximosPermitidos().contains(novo);
    }

    /** Status que representam a saída do pedido (exigem data/hora e responsável). */
    public boolean isSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    /** O pedido já saiu da pizzaria? (a partir daqui não cabe desconto nem cancelamento) */
    public boolean jaSaiu() {
        return isSaida() || this == FINALIZADO;
    }

    public boolean isFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }
}
