package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioAtual;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes das regras de negócio do PedidoService (RF02, RF03, RF04),
 * com repositórios simulados (Mockito). Não acessam o MongoDB.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private UsuarioAtual usuarioAtual;

    @InjectMocks
    private PedidoService pedidoService;

    private Cliente cliente;
    private Produto pizza;

    @BeforeEach
    void preparar() {
        cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("João");

        pizza = new Produto();
        pizza.setId("p1");
        pizza.setNome("Calabresa");
        pizza.setPreco(45.90);
        pizza.setAtivo(true);

        lenient().when(usuarioAtual.username()).thenReturn("atendente01");
        lenient().when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));
        lenient().when(produtoRepository.findById("p1")).thenReturn(Optional.of(pizza));
        lenient().when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pedido pedidoNoStatus(StatusPedido status) {
        Pedido pedido = pedidoService.criarPedido(
                new PedidoRequest("c1", TipoAtendimento.ENTREGA, null, List.of(new ItemPedidoRequest("p1", 2))));
        pedido.setId("ped1");
        pedido.setStatus(status);
        when(pedidoRepository.findById("ped1")).thenReturn(Optional.of(pedido));
        return pedido;
    }

    @Test
    void calculaTotalNoServidorComPrecoDoCardapio() {
        Pedido pedido = pedidoService.criarPedido(
                new PedidoRequest("c1", TipoAtendimento.ENTREGA, null, List.of(new ItemPedidoRequest("p1", 2))));

        assertEquals(0, new BigDecimal("91.80").compareTo(pedido.getValorTotal()));
        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        assertEquals("atendente01", pedido.getResponsavelEntrada());
        assertNotNull(pedido.getDataHoraEntrada());
        verify(auditoriaService).registrar(eq(TipoOperacao.CRIACAO), eq("Pedido"), any(), anyList(), isNull());
    }

    @Test
    void naoPermiteFinalizarPedidoEmPreparacao() {
        pedidoNoStatus(StatusPedido.EM_PREPARACAO);
        assertThrows(RegraNegocioException.class,
                () -> pedidoService.atualizarStatus("ped1", StatusPedido.FINALIZADO));
    }

    @Test
    void saidaRegistraDataHoraEResponsavel() {
        Pedido pedido = pedidoNoStatus(StatusPedido.PRONTO);
        pedidoService.atualizarStatus("ped1", StatusPedido.SAIU_PARA_ENTREGA);

        assertNotNull(pedido.getDataHoraSaida());
        assertEquals("atendente01", pedido.getResponsavelSaida());
    }

    @Test
    void naoPermiteCancelarPelaRotaDeStatus() {
        pedidoNoStatus(StatusPedido.RECEBIDO);
        assertThrows(RegraNegocioException.class,
                () -> pedidoService.atualizarStatus("ped1", StatusPedido.CANCELADO));
    }

    @Test
    void cancelamentoExigeJustificativa() {
        // A justificativa é validada antes mesmo de buscar o pedido
        assertThrows(RegraNegocioException.class, () -> pedidoService.cancelar("ped1", " "));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void naoCancelaDepoisDaSaida() {
        pedidoNoStatus(StatusPedido.SAIU_PARA_ENTREGA);
        assertThrows(RegraNegocioException.class,
                () -> pedidoService.cancelar("ped1", "Cliente desistiu do pedido"));
    }

    @Test
    void cancelamentoPreservaDadosEAudita() {
        Pedido pedido = pedidoNoStatus(StatusPedido.EM_PREPARACAO);
        pedidoService.cancelar("ped1", "Cliente desistiu do pedido");

        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        assertEquals(1, pedido.getItens().size());
        assertEquals(0, new BigDecimal("91.80").compareTo(pedido.getValorTotal()));
        verify(auditoriaService).registrar(eq(TipoOperacao.CANCELAMENTO), eq("Pedido"), eq("ped1"), anyList(),
                eq("Cliente desistiu do pedido"));
    }

    @Test
    void naoAlteraStatusDePedidoCancelado() {
        pedidoNoStatus(StatusPedido.CANCELADO);
        assertThrows(RegraNegocioException.class,
                () -> pedidoService.atualizarStatus("ped1", StatusPedido.EM_PREPARACAO));
    }

    @Test
    void descontoDe20PorCentoEhAceito() {
        Pedido pedido = pedidoNoStatus(StatusPedido.RECEBIDO);
        pedidoService.aplicarDesconto("ped1", new BigDecimal("20"), null);

        assertEquals(0, new BigDecimal("18.36").compareTo(pedido.getValorDesconto()));
        assertEquals(0, new BigDecimal("73.44").compareTo(pedido.getValorTotal()));
    }

    @Test
    void descontoAcimaDe20PorCentoEhRecusado() {
        assertThrows(RegraNegocioException.class,
                () -> pedidoService.aplicarDesconto("ped1", new BigDecimal("25"), null));
        verify(pedidoRepository, never()).findById(anyString());
    }

    @Test
    void descontoDepoisDaSaidaEhRecusado() {
        pedidoNoStatus(StatusPedido.RETIRADO);
        assertThrows(RegraNegocioException.class,
                () -> pedidoService.aplicarDesconto("ped1", new BigDecimal("10"), null));
    }
}
