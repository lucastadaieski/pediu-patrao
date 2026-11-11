package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Indicadores gerais do período (RF01), com a comparação com o período anterior.
 *
 * Regra de negócio: pedidos CANCELADOS não entram no faturamento, no ticket médio
 * nem nos itens vendidos; entram só na contagem de cancelados e na taxa de cancelamento.
 */
public record ResumoVendas(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal faturamento,
        long totalPedidos,
        long pedidosValidos,
        long pedidosCancelados,
        BigDecimal taxaCancelamento,   // % sobre o total de pedidos
        BigDecimal ticketMedio,        // faturamento / pedidos válidos
        long itensVendidos,
        BigDecimal totalDescontos,
        Comparativo comparativo,
        LocalDateTime atualizadoEm) {

    /**
     * Mesmos indicadores no período anterior de igual duração.
     * As variações são percentuais e ficam null quando o período anterior é zero
     * (não existe "variação" a partir de zero).
     */
    public record Comparativo(
            LocalDate inicio,
            LocalDate fim,
            BigDecimal faturamento,
            long pedidosValidos,
            BigDecimal ticketMedio,
            BigDecimal taxaCancelamento,
            BigDecimal variacaoFaturamento,
            BigDecimal variacaoPedidos,
            BigDecimal variacaoTicketMedio,
            BigDecimal diferencaTaxaCancelamento) { // em pontos percentuais, não em %
    }
}
