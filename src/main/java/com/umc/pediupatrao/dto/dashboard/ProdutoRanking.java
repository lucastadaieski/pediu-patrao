package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record ProdutoRanking(int posicao, String produtoId, String nome, String categoria, long quantidade,
                             BigDecimal faturamento, BigDecimal percentualFaturamento) {
}
