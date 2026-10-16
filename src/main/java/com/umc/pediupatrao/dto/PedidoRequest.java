package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PedidoRequest(
        @NotBlank(message = "Informe o cliente") String clienteId,
        @NotNull(message = "Informe se é entrega ou retirada") TipoAtendimento tipoAtendimento,
        @Size(max = 500, message = "Observação com no máximo 500 caracteres") String observacao,
        @NotEmpty(message = "O pedido precisa de ao menos um item") List<@Valid ItemPedidoRequest> itens) {
}
