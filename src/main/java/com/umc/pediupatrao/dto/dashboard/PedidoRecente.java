package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record PedidoRecente(String id, String clienteNome, String tipoAtendimento, String tipoAtendimentoDescricao,
                            String status, String statusDescricao, int quantidadeItens, BigDecimal valorTotal,
                            LocalDateTime dataHoraEntrada, long minutosDesdeEntrada, boolean atrasado) {
}
