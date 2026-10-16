package com.umc.pediupatrao.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.enums.StatusPedido;
import org.junit.jupiter.api.Test;

/** Testes do fluxo obrigatório de status (RF03). Não precisam de banco nem de Spring. */
class StatusPedidoTest {

    @Test
    void fluxoNormalDeEntrega() {
        assertTrue(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.EM_PREPARACAO));
        assertTrue(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.PRONTO));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.SAIU_PARA_ENTREGA));
        assertTrue(StatusPedido.SAIU_PARA_ENTREGA.podeIrPara(StatusPedido.FINALIZADO));
    }

    @Test
    void naoPermiteFinalizarPedidoEmPreparacao() {
        assertFalse(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.FINALIZADO));
    }

    @Test
    void naoPermitePularEtapas() {
        assertFalse(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.PRONTO));
        assertFalse(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.SAIU_PARA_ENTREGA));
    }

    @Test
    void cancelamentoSomenteAntesDaSaida() {
        assertTrue(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.CANCELADO));
        assertTrue(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.CANCELADO));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.CANCELADO));
        assertFalse(StatusPedido.SAIU_PARA_ENTREGA.podeIrPara(StatusPedido.CANCELADO));
        assertFalse(StatusPedido.RETIRADO.podeIrPara(StatusPedido.CANCELADO));
    }

    @Test
    void estadosFinaisNaoTemTransicao() {
        assertTrue(StatusPedido.CANCELADO.proximosPermitidos().isEmpty());
        assertTrue(StatusPedido.FINALIZADO.proximosPermitidos().isEmpty());
    }
}
