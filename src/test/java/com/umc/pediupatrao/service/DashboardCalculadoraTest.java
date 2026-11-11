package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FatiaVendas;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.PedidoRecente;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Testes dos cálculos do dashboard (RF01 a RF04) com pedidos montados na mão.
 * Não acessam o MongoDB.
 */
class DashboardCalculadoraTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);   // domingo
    private static final LocalDate FIM = LocalDate.of(2026, 11, 10);     // terça
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 20, 0);
    private static final FiltroDashboard FILTRO = new FiltroDashboard(INICIO, FIM, null, null);

    private final DashboardCalculadora calc = new DashboardCalculadora();

    // ---------- RF01 ----------
    @Test
    void canceladoNaoEntraNoFaturamentoMasEntraNaTaxa() {
        List<Pedido> pedidos = List.of(
                pedido("100.00", StatusPedido.FINALIZADO, dia(2, 19)),
                pedido("50.00", StatusPedido.FINALIZADO, dia(3, 20)),
                pedido("80.00", StatusPedido.CANCELADO, dia(3, 21)),
                pedido("30.00", StatusPedido.EM_PREPARACAO, dia(10, 19)));

        ResumoVendas r = calc.resumo(FILTRO, pedidos, FILTRO.periodoAnterior(), List.of(), AGORA);

        assertEquals(new BigDecimal("180.00"), r.faturamento());
        assertEquals(4, r.totalPedidos());
        assertEquals(3, r.pedidosValidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(new BigDecimal("25.0"), r.taxaCancelamento());
        assertEquals(new BigDecimal("60.00"), r.ticketMedio());
        assertEquals(3, r.itensVendidos()); // cada pedido de teste tem 1 item; o cancelado não conta
    }

    @Test
    void comparaComOPeriodoAnterior() {
        List<Pedido> atual = List.of(pedido("150.00", StatusPedido.FINALIZADO, dia(2, 19)));
        List<Pedido> anterior = List.of(
                pedido("50.00", StatusPedido.FINALIZADO, LocalDateTime.of(2026, 10, 25, 19, 0)),
                pedido("50.00", StatusPedido.CANCELADO, LocalDateTime.of(2026, 10, 26, 19, 0)));

        ResumoVendas.Comparativo c = calc.resumo(FILTRO, atual, FILTRO.periodoAnterior(), anterior, AGORA).comparativo();

        assertEquals(new BigDecimal("50.00"), c.faturamento());
        assertEquals(new BigDecimal("200.0"), c.variacaoFaturamento());      // 50 -> 150 = +200%
        assertEquals(new BigDecimal("0.0"), c.variacaoPedidos());            // 1 -> 1
        assertEquals(new BigDecimal("-50.0"), c.diferencaTaxaCancelamento()); // 50% -> 0% = -50 p.p.
    }

    @Test
    void semPeriodoAnteriorAVariacaoFicaNula() {
        ResumoVendas.Comparativo c = calc.resumo(FILTRO, List.of(pedido("10.00", StatusPedido.FINALIZADO, dia(2, 12))),
                FILTRO.periodoAnterior(), List.of(), AGORA).comparativo();
        assertNull(c.variacaoFaturamento());
        assertNull(c.variacaoTicketMedio());
        assertNull(c.diferencaTaxaCancelamento());
    }

    @Test
    void periodoSemPedidosDevolveZerosSemErro() {
        ResumoVendas r = calc.resumo(FILTRO, List.of(), FILTRO.periodoAnterior(), List.of(), AGORA);
        assertEquals(new BigDecimal("0.00"), r.faturamento());
        assertEquals(new BigDecimal("0.00"), r.ticketMedio());
        assertEquals(new BigDecimal("0.0"), r.taxaCancelamento());
    }

    @Test
    void valorNuloNaoQuebraOCalculo() {
        Pedido antigo = pedido("10.00", StatusPedido.FINALIZADO, dia(2, 12));
        antigo.setValorTotal(null);
        ResumoVendas r = calc.resumo(FILTRO, List.of(antigo), FILTRO.periodoAnterior(), List.of(), AGORA);
        assertEquals(new BigDecimal("0.00"), r.faturamento());
    }

    // ---------- RF02 ----------
    @Test
    void serieDiariaTemTodosOsDiasMesmoSemVenda() {
        List<PontoSerie> serie = calc.serie(FILTRO, List.of(
                pedido("40.00", StatusPedido.FINALIZADO, dia(1, 19)),
                pedido("60.00", StatusPedido.FINALIZADO, dia(1, 21)),
                pedido("99.00", StatusPedido.CANCELADO, dia(5, 21))), Agrupamento.DIA);

        assertEquals(10, serie.size());
        assertEquals("01/11", serie.get(0).rotulo());
        assertEquals(new BigDecimal("100.00"), serie.get(0).faturamento());
        assertEquals(2, serie.get(0).pedidos());
        assertEquals(new BigDecimal("50.00"), serie.get(0).ticketMedio());
        assertEquals(0, serie.get(1).pedidos());
        assertEquals(1, serie.get(4).cancelados());
        assertEquals(new BigDecimal("0.00"), serie.get(4).faturamento());
    }

    @Test
    void seriePorSemanaCortaNasBordasDoFiltro() {
        // 01/11/2026 é domingo: a 1ª semana (seg 26/10) aparece cortada só com o dia 01/11
        List<PontoSerie> serie = calc.serie(FILTRO, List.of(
                pedido("10.00", StatusPedido.FINALIZADO, dia(1, 12)),
                pedido("20.00", StatusPedido.FINALIZADO, dia(2, 12)),
                pedido("30.00", StatusPedido.FINALIZADO, dia(9, 12))), Agrupamento.SEMANA);

        assertEquals(3, serie.size());
        assertEquals(INICIO, serie.get(0).inicio());
        assertEquals(INICIO, serie.get(0).fim());
        assertEquals(new BigDecimal("10.00"), serie.get(0).faturamento());
        assertEquals(LocalDate.of(2026, 11, 2), serie.get(1).inicio());
        assertEquals(LocalDate.of(2026, 11, 8), serie.get(1).fim());
        assertEquals(FIM, serie.get(2).fim());
        assertEquals(new BigDecimal("30.00"), serie.get(2).faturamento());
    }

    @Test
    void seriePorMes() {
        FiltroDashboard tri = new FiltroDashboard(LocalDate.of(2026, 9, 15), FIM, null, null);
        List<PontoSerie> serie = calc.serie(tri, List.of(
                pedido("10.00", StatusPedido.FINALIZADO, LocalDateTime.of(2026, 9, 20, 12, 0)),
                pedido("20.00", StatusPedido.FINALIZADO, LocalDateTime.of(2026, 10, 3, 12, 0))), Agrupamento.MES);
        assertEquals(3, serie.size());
        assertEquals("09/2026", serie.get(0).rotulo());
        assertEquals(LocalDate.of(2026, 9, 15), serie.get(0).inicio());
        assertEquals(new BigDecimal("20.00"), serie.get(1).faturamento());
    }

    @Test
    void porHoraTem24FaixasEIgnoraCancelados() {
        List<FatiaVendas> horas = calc.porHora(List.of(
                pedido("50.00", StatusPedido.FINALIZADO, dia(2, 19)),
                pedido("50.00", StatusPedido.FINALIZADO, dia(3, 19)),
                pedido("10.00", StatusPedido.CANCELADO, dia(3, 19))));
        assertEquals(24, horas.size());
        assertEquals("19h", horas.get(19).rotulo());
        assertEquals(2, horas.get(19).pedidos());
        assertEquals(new BigDecimal("100.0"), horas.get(19).percentual());
    }

    @Test
    void porDiaSemanaComecaNaSegunda() {
        List<FatiaVendas> dias = calc.porDiaSemana(List.of(pedido("50.00", StatusPedido.FINALIZADO, dia(1, 19))));
        assertEquals(7, dias.size());
        assertEquals("Seg", dias.get(0).rotulo());
        assertEquals(1, dias.get(6).pedidos()); // 01/11/2026 é domingo
    }

    @Test
    void porTipoDeAtendimentoCalculaParticipacao() {
        Pedido entrega = pedido("75.00", StatusPedido.FINALIZADO, dia(2, 19));
        Pedido retirada = pedido("25.00", StatusPedido.FINALIZADO, dia(2, 20));
        retirada.setTipoAtendimento(TipoAtendimento.RETIRADA);
        List<FatiaVendas> fatias = calc.porTipoAtendimento(List.of(entrega, retirada));
        assertEquals("ENTREGA", fatias.get(0).chave());
        assertEquals(new BigDecimal("75.0"), fatias.get(0).percentual());
        assertEquals(new BigDecimal("25.0"), fatias.get(1).percentual());
    }

    // ---------- RF03 ----------
    @Test
    void situacaoContaAndamentoAtrasadosETempoMedio() {
        Pedido finalizado = pedido("10.00", StatusPedido.FINALIZADO, dia(9, 19));
        finalizado.setDataHoraFinalizacao(dia(9, 19).plusMinutes(50));
        Pedido finalizado2 = pedido("10.00", StatusPedido.FINALIZADO, dia(9, 20));
        finalizado2.setDataHoraFinalizacao(dia(9, 20).plusMinutes(30));
        Pedido atrasado = pedido("10.00", StatusPedido.EM_PREPARACAO, AGORA.minusMinutes(55));
        Pedido noPrazo = pedido("10.00", StatusPedido.RECEBIDO, AGORA.minusMinutes(5));
        Pedido cancelado = pedido("10.00", StatusPedido.CANCELADO, AGORA.minusMinutes(90));

        SituacaoPedidos s = calc.situacao(List.of(finalizado, finalizado2, atrasado, noPrazo, cancelado), AGORA, 40);

        assertEquals(5, s.total());
        assertEquals(2, s.emAndamento());
        assertEquals(1, s.atrasados());
        assertEquals(2, s.finalizados());
        assertEquals(1, s.cancelados());
        assertEquals(new BigDecimal("40.0"), s.tempoMedioMinutos());
        assertEquals(StatusPedido.values().length, s.porStatus().size());
        assertEquals(new BigDecimal("40.0"), s.porStatus().get(StatusPedido.FINALIZADO.ordinal()).percentual());
    }

    @Test
    void recentesVemDoMaisNovoParaOMaisAntigoComLimite() {
        Pedido velho = pedido("10.00", StatusPedido.FINALIZADO, dia(2, 12));
        Pedido novo = pedido("20.00", StatusPedido.EM_PREPARACAO, AGORA.minusMinutes(50));
        Pedido medio = pedido("30.00", StatusPedido.FINALIZADO, dia(5, 12));
        List<PedidoRecente> r = calc.recentes(List.of(velho, novo, medio), 2, AGORA, 40);
        assertEquals(2, r.size());
        assertEquals("p-20.00", r.get(0).id());
        assertTrue(r.get(0).atrasado());
        assertEquals(50, r.get(0).minutosDesdeEntrada());
        assertFalse(r.get(1).atrasado()); // finalizado nunca está atrasado
    }

    // ---------- RF04 ----------
    @Test
    void rankingSomaPorProdutoEOrdenaPeloCriterio() {
        Pedido p1 = pedidoComItens(StatusPedido.FINALIZADO,
                new ItemPedido("piz", "Pizza Calabresa", new BigDecimal("50.00"), 1),
                new ItemPedido("ref", "Refrigerante", new BigDecimal("10.00"), 3));
        Pedido p2 = pedidoComItens(StatusPedido.FINALIZADO,
                new ItemPedido("piz", "Pizza Calabresa", new BigDecimal("50.00"), 1));
        Pedido cancelado = pedidoComItens(StatusPedido.CANCELADO,
                new ItemPedido("ref", "Refrigerante", new BigDecimal("10.00"), 10));
        Map<String, String> categorias = Map.of("piz", "Pizza", "ref", "Bebida");

        List<ProdutoRanking> porQtd = calc.ranking(List.of(p1, p2, cancelado), categorias, CriterioRanking.QUANTIDADE, 10, null);
        assertEquals("Refrigerante", porQtd.get(0).nome());
        assertEquals(3, porQtd.get(0).quantidade()); // os 10 do cancelado não contam
        assertEquals("Bebida", porQtd.get(0).categoria());

        List<ProdutoRanking> porFat = calc.ranking(List.of(p1, p2), categorias, CriterioRanking.FATURAMENTO, 10, null);
        assertEquals("Pizza Calabresa", porFat.get(0).nome());
        assertEquals(new BigDecimal("100.00"), porFat.get(0).faturamento());
        assertEquals(new BigDecimal("76.9"), porFat.get(0).percentualFaturamento()); // 100 de 130
        assertEquals(1, porFat.get(0).posicao());
    }

    @Test
    void rankingRespeitaLimiteECategoria() {
        Pedido p = pedidoComItens(StatusPedido.FINALIZADO,
                new ItemPedido("a", "A", new BigDecimal("1.00"), 3),
                new ItemPedido("b", "B", new BigDecimal("1.00"), 2),
                new ItemPedido("c", "C", new BigDecimal("1.00"), 1));
        Map<String, String> cat = Map.of("a", "Pizza", "b", "Bebida", "c", "Pizza");

        assertEquals(2, calc.ranking(List.of(p), cat, CriterioRanking.QUANTIDADE, 2, null).size());
        List<ProdutoRanking> soPizza = calc.ranking(List.of(p), cat, CriterioRanking.QUANTIDADE, 10, "pizza");
        assertEquals(2, soPizza.size());
        assertEquals("C", soPizza.get(1).nome());
    }

    @Test
    void produtoExcluidoDoCadastroAindaApareceSemCategoria() {
        Pedido p = pedidoComItens(StatusPedido.FINALIZADO, new ItemPedido("x", "Antigo", new BigDecimal("5.00"), 1));
        assertEquals("Sem categoria", calc.ranking(List.of(p), Map.of(), CriterioRanking.QUANTIDADE, 10, null)
                .get(0).categoria());
    }

    // ---------- Auxiliares ----------
    private static LocalDateTime dia(int diaDoMes, int hora) {
        return LocalDateTime.of(2026, 11, diaDoMes, hora, 0);
    }

    private static Pedido pedido(String total, StatusPedido status, LocalDateTime entrada) {
        BigDecimal valor = new BigDecimal(total);
        Pedido p = new Pedido();
        p.setId("p-" + total);
        p.setClienteNome("Cliente");
        p.setTipoAtendimento(TipoAtendimento.ENTREGA);
        p.setStatus(status);
        p.setDataHoraEntrada(entrada);
        p.setItens(new java.util.ArrayList<>(List.of(new ItemPedido("prod", "Produto", valor, 1))));
        p.setSubtotal(valor);
        p.setValorDesconto(BigDecimal.ZERO);
        p.setValorTotal(valor);
        return p;
    }

    private static Pedido pedidoComItens(StatusPedido status, ItemPedido... itens) {
        Pedido p = pedido("0.00", status, dia(3, 19));
        p.setItens(List.of(itens));
        return p;
    }
}
