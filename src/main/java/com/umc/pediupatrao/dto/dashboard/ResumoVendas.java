package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
