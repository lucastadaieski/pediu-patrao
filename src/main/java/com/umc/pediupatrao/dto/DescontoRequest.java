package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DescontoRequest(
        @NotNull(message = "Informe o percentual de desconto")
        @DecimalMin(value = "0", inclusive = false, message = "O desconto deve ser maior que 0%")
        @DecimalMax(value = "20", message = "O desconto é limitado a 20%")
        BigDecimal percentual,
        @Size(max = 500, message = "Justificativa com no máximo 500 caracteres")
        String justificativa) {
}
