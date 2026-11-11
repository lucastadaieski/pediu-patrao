package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

/**
 * Uma linha do ranking de produtos mais vendidos (RF04).
 *
 * "faturamento" soma o subtotal dos itens (preço gravado no pedido x quantidade),
 * antes do desconto do pedido, porque o desconto é dado no pedido inteiro e não por produto.
 */
public record ProdutoRanking(int posicao, String produtoId, String nome, String categoria, long quantidade,
                             BigDecimal faturamento, BigDecimal percentualFaturamento) {
}
