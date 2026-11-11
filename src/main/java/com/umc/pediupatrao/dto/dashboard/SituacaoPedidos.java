package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Situação dos pedidos do período (RF03).
 *
 * "emAndamento" são os pedidos ainda não finalizados nem cancelados;
 * "atrasados" são os em andamento com mais de "minutosAtraso" desde a entrada.
 * "tempoMedioMinutos" vai da entrada à finalização (só pedidos FINALIZADOS); null se não houver.
 */
public record SituacaoPedidos(long total, long emAndamento, long finalizados, long cancelados, long atrasados,
                              int minutosAtraso, BigDecimal tempoMedioMinutos, List<QuantidadeStatus> porStatus) {

    public record QuantidadeStatus(String status, String descricao, long quantidade, BigDecimal percentual) {
    }
}
