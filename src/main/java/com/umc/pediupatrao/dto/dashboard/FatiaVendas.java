package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

/**
 * Vendas de uma categoria de agrupamento (hora do dia, dia da semana ou tipo de atendimento).
 * "percentual" é a participação dessa fatia no faturamento total do período.
 */
public record FatiaVendas(String chave, String rotulo, long pedidos, BigDecimal faturamento,
                          BigDecimal percentual) {
}
