package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;


public record SituacaoPedidos(long total, long emAndamento, long finalizados, long cancelados, long atrasados,
                              int minutosAtraso, BigDecimal tempoMedioMinutos, List<QuantidadeStatus> porStatus) {

    public record QuantidadeStatus(String status, String descricao, long quantidade, BigDecimal percentual) {
    }
}
