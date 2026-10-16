package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemPedidoRequest(
        @NotBlank(message = "Informe o produto do item") String produtoId,
        @Min(value = 1, message = "A quantidade mínima é 1") @Max(value = 99, message = "A quantidade máxima é 99") int quantidade) {
}
