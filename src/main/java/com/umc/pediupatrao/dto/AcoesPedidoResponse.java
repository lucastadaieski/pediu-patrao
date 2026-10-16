package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.enums.StatusPedido;
import java.math.BigDecimal;
import java.util.List;


public record AcoesPedidoResponse(List<OpcaoStatus> proximosStatus, boolean podeCancelar,
                                  boolean podeAplicarDesconto, BigDecimal descontoMaximo) {

    public record OpcaoStatus(StatusPedido status, String descricao) {
    }
}
