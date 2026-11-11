package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;


public record PontoSerie(String rotulo, LocalDate inicio, LocalDate fim, long pedidos, long cancelados,
                         BigDecimal faturamento, BigDecimal ticketMedio) {
}
