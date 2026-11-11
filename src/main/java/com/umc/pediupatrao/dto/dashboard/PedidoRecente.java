package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumo de um pedido para a tabela de últimos pedidos do dashboard (RF03).
 * Só os campos que a tela precisa: não expõe responsáveis, justificativas etc.
 */
public record PedidoRecente(String id, String clienteNome, String tipoAtendimento, String tipoAtendimentoDescricao,
                            String status, String statusDescricao, int quantidadeItens, BigDecimal valorTotal,
                            LocalDateTime dataHoraEntrada, long minutosDesdeEntrada, boolean atrasado) {
}
