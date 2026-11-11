package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um ponto da série temporal de vendas (RF02). Os períodos sem pedidos também aparecem,
 * com zero, para o gráfico não "pular" dias.
 */
public record PontoSerie(String rotulo, LocalDate inicio, LocalDate fim, long pedidos, long cancelados,
                         BigDecimal faturamento, BigDecimal ticketMedio) {
}
