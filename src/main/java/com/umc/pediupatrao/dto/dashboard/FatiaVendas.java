package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record FatiaVendas(String chave, String rotulo, long pedidos, BigDecimal faturamento,
                          BigDecimal percentual) {
}
