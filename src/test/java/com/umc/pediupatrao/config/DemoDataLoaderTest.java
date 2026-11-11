package com.umc.pediupatrao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Garante que os pedidos de demonstração respeitam as mesmas regras dos pedidos reais. */
class DemoDataLoaderTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 19, 30);

    @Test
    void pedidosDemoSaoCoerentes() {
        Produto pizza = new Produto();
        pizza.setId("piz");
        pizza.setNome("Pizza");
        pizza.setPreco(49.90);
        pizza.setAtivo(true);
        Cliente cliente = new Cliente();
        cliente.setId("cli");
        cliente.setNome("Ana");

        List<Pedido> pedidos = DemoDataLoader.gerar(List.of(pizza), List.of(cliente), AGORA, 60, new Random(42));

        assertTrue(pedidos.size() > 200, "deveria gerar algumas centenas de pedidos");
        for (Pedido p : pedidos) {
            assertFalse(p.getDataHoraEntrada().isAfter(AGORA), "pedido no futuro");
            assertTrue(p.getDataHoraEntrada().isAfter(AGORA.minusDays(60)));
            assertNotNull(p.getStatus());
            assertEquals(DemoDataLoader.RESPONSAVEL_DEMO, p.getResponsavelEntrada());

            BigDecimal soma = p.getItens().stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, soma.compareTo(p.getSubtotal()), "subtotal diferente da soma dos itens");
            assertEquals(0, p.getSubtotal().subtract(p.getValorDesconto()).compareTo(p.getValorTotal()));
            assertTrue(p.getPercentualDesconto().compareTo(new BigDecimal("20")) <= 0);

            if (p.getStatus() == StatusPedido.FINALIZADO) {
                assertNotNull(p.getDataHoraSaida());
                assertTrue(p.getDataHoraFinalizacao().isAfter(p.getDataHoraSaida()));
                assertFalse(p.getDataHoraFinalizacao().isAfter(AGORA));
            }
            if (p.getStatus() == StatusPedido.CANCELADO) {
                assertNotNull(p.getJustificativaCancelamento());
                assertEquals(null, p.getDataHoraSaida()); // cancelamento só antes da saída
            }
        }
        assertTrue(pedidos.stream().anyMatch(p -> p.getStatus() == StatusPedido.CANCELADO));
        assertTrue(pedidos.stream().anyMatch(p -> !p.getStatus().isFinal()), "deveria haver pedidos em andamento");
    }
}
