package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FatiaVendas;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.PedidoRecente;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos.QuantidadeStatus;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DashboardCalculadora {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final String[] DIAS_SEMANA = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

    // ========================
    // RF01: indicadores gerais
    // ========================
    public ResumoVendas resumo(FiltroDashboard filtro, List<Pedido> atual, FiltroDashboard filtroAnterior,
                               List<Pedido> anterior, LocalDateTime agora) {
        Totais a = Totais.de(atual);
        Totais b = Totais.de(anterior);

        ResumoVendas.Comparativo comparativo = new ResumoVendas.Comparativo(
                filtroAnterior.inicio(), filtroAnterior.fim(),
                b.faturamento, b.validos, b.ticketMedio(), b.taxaCancelamento(),
                variacao(a.faturamento, b.faturamento),
                variacao(BigDecimal.valueOf(a.validos), BigDecimal.valueOf(b.validos)),
                variacao(a.ticketMedio(), b.ticketMedio()),
                // taxa já é percentual: a diferença vai em pontos percentuais (ex.: 5% -> 8% = +3,0 p.p.)
                b.total == 0 ? null : a.taxaCancelamento().subtract(b.taxaCancelamento()));

        return new ResumoVendas(filtro.inicio(), filtro.fim(), a.faturamento, a.total, a.validos, a.cancelados,
                a.taxaCancelamento(), a.ticketMedio(), a.itens, a.descontos, comparativo, agora);
    }

    // ========================
    // RF02: série temporal
    // ========================
    public List<PontoSerie> serie(FiltroDashboard filtro, List<Pedido> pedidos, Agrupamento agrupamento) {
        Agrupamento ag = agrupamento == null ? Agrupamento.DIA : agrupamento;

        // 1) Cria todos os "baldes" do período, inclusive os vazios, em ordem
        Map<LocalDate, List<Pedido>> baldes = new LinkedHashMap<>();
        LocalDate cursor = inicioDoBalde(filtro.inicio(), ag);
        while (!cursor.isAfter(filtro.fim())) {
            baldes.put(cursor, new ArrayList<>());
            cursor = proximoBalde(cursor, ag);
        }

        // 2) Distribui os pedidos
        for (Pedido p : pedidos) {
            if (p.getDataHoraEntrada() == null) {
                continue;
            }
            List<Pedido> balde = baldes.get(inicioDoBalde(p.getDataHoraEntrada().toLocalDate(), ag));
            if (balde != null) {
                balde.add(p);
            }
        }

        // 3) Totaliza cada balde. O primeiro e o último são cortados nas bordas do filtro
        List<PontoSerie> pontos = new ArrayList<>();
        baldes.forEach((inicioBalde, lista) -> {
            LocalDate ini = max(inicioBalde, filtro.inicio());
            LocalDate fim = min(proximoBalde(inicioBalde, ag).minusDays(1), filtro.fim());
            Totais t = Totais.de(lista);
            pontos.add(new PontoSerie(rotulo(inicioBalde, ini, ag), ini, fim, t.validos, t.cancelados,
                    t.faturamento, t.ticketMedio()));
        });
        return pontos;
    }

    // ========================
    // RF02: distribuições
    // ========================
    public List<FatiaVendas> porHora(List<Pedido> pedidos) {
        List<List<Pedido>> horas = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            horas.add(new ArrayList<>());
        }
        for (Pedido p : validos(pedidos)) {
            if (p.getDataHoraEntrada() != null) {
                horas.get(p.getDataHoraEntrada().getHour()).add(p);
            }
        }
        BigDecimal total = Totais.de(pedidos).faturamento;
        List<FatiaVendas> fatias = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            fatias.add(fatia(String.valueOf(h), String.format("%02dh", h), horas.get(h), total));
        }
        return fatias;
    }

    public List<FatiaVendas> porDiaSemana(List<Pedido> pedidos) {
        Map<DayOfWeek, List<Pedido>> dias = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) { // começa na segunda-feira
            dias.put(d, new ArrayList<>());
        }
        for (Pedido p : validos(pedidos)) {
            if (p.getDataHoraEntrada() != null) {
                dias.get(p.getDataHoraEntrada().getDayOfWeek()).add(p);
            }
        }
        BigDecimal total = Totais.de(pedidos).faturamento;
        List<FatiaVendas> fatias = new ArrayList<>();
        dias.forEach((d, lista) -> fatias.add(fatia(d.name(), DIAS_SEMANA[d.getValue() - 1], lista, total)));
        return fatias;
    }

    public List<FatiaVendas> porTipoAtendimento(List<Pedido> pedidos) {
        BigDecimal total = Totais.de(pedidos).faturamento;
        List<FatiaVendas> fatias = new ArrayList<>();
        for (TipoAtendimento tipo : TipoAtendimento.values()) {
            List<Pedido> doTipo = validos(pedidos).stream().filter(p -> p.getTipoAtendimento() == tipo).toList();
            fatias.add(fatia(tipo.name(), tipo.getDescricao(), doTipo, total));
        }
        return fatias;
    }

    // ========================
    // RF03: situação dos pedidos
    // ========================
    public SituacaoPedidos situacao(List<Pedido> pedidos, LocalDateTime agora, int minutosAtraso) {
        long total = pedidos.size();
        List<QuantidadeStatus> porStatus = new ArrayList<>();
        for (StatusPedido s : StatusPedido.values()) {
            long qtd = pedidos.stream().filter(p -> p.getStatus() == s).count();
            porStatus.add(new QuantidadeStatus(s.name(), s.getDescricao(), qtd, percentual(qtd, total)));
        }

        long emAndamento = pedidos.stream().filter(this::emAndamento).count();
        long atrasados = pedidos.stream().filter(p -> atrasado(p, agora, minutosAtraso)).count();
        long finalizados = pedidos.stream().filter(p -> p.getStatus() == StatusPedido.FINALIZADO).count();
        long cancelados = pedidos.stream().filter(Pedido::isCancelado).count();

        List<Long> tempos = pedidos.stream()
                .filter(p -> p.getStatus() == StatusPedido.FINALIZADO)
                .filter(p -> p.getDataHoraEntrada() != null && p.getDataHoraFinalizacao() != null)
                .map(p -> Duration.between(p.getDataHoraEntrada(), p.getDataHoraFinalizacao()).toMinutes())
                .filter(min -> min >= 0)
                .toList();
        BigDecimal tempoMedio = tempos.isEmpty() ? null
                : BigDecimal.valueOf(tempos.stream().mapToLong(Long::longValue).sum())
                        .divide(BigDecimal.valueOf(tempos.size()), 1, RoundingMode.HALF_UP);

        return new SituacaoPedidos(total, emAndamento, finalizados, cancelados, atrasados, minutosAtraso,
                tempoMedio, porStatus);
    }

    public List<PedidoRecente> recentes(List<Pedido> pedidos, int limite, LocalDateTime agora, int minutosAtraso) {
        return pedidos.stream()
                .filter(p -> p.getDataHoraEntrada() != null)
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrada).reversed())
                .limit(limite)
                .map(p -> new PedidoRecente(
                        p.getId(),
                        p.getClienteNome() == null || p.getClienteNome().isBlank() ? "(sem nome)" : p.getClienteNome(),
                        p.getTipoAtendimento() == null ? null : p.getTipoAtendimento().name(),
                        p.getTipoAtendimentoDescricao(),
                        p.getStatus() == null ? null : p.getStatus().name(),
                        p.getStatusDescricao(),
                        p.getQuantidadeItens(),
                        valor(p.getValorTotal()),
                        p.getDataHoraEntrada(),
                        Math.max(0, Duration.between(p.getDataHoraEntrada(), agora).toMinutes()),
                        atrasado(p, agora, minutosAtraso)))
                .toList();
    }

    // ========================
    // RF04: produtos mais vendidos
    // ========================
    /**
     * @param categorias mapa produtoId -> categoria (Produto.tipo), vindo do cadastro atual
     * @param categoria  se informada, só entram produtos dessa categoria
     */
    public List<ProdutoRanking> ranking(List<Pedido> pedidos, Map<String, String> categorias,
                                        CriterioRanking criterio, int limite, String categoria) {
        // Acumula por produto. Item sem produtoId (dado antigo) agrupa pelo nome.
        Map<String, Acumulado> porProduto = new LinkedHashMap<>();
        for (Pedido p : validos(pedidos)) {
            if (p.getItens() == null) {
                continue;
            }
            for (ItemPedido item : p.getItens()) {
                if (item == null || item.getQuantidade() <= 0) {
                    continue;
                }
                String chave = item.getProdutoId() != null ? item.getProdutoId() : "nome:" + item.getNomeProduto();
                Acumulado acc = porProduto.computeIfAbsent(chave, k -> new Acumulado(item.getProdutoId()));
                acc.nome = item.getNomeProduto() != null ? item.getNomeProduto() : acc.nome;
                acc.quantidade += item.getQuantidade();
                acc.faturamento = acc.faturamento.add(subtotalItem(item));
            }
        }

        List<Acumulado> lista = new ArrayList<>(porProduto.values());
        for (Acumulado acc : lista) {
            String cat = acc.produtoId == null ? null : categorias.get(acc.produtoId);
            acc.categoria = cat == null || cat.isBlank() ? "Sem categoria" : cat;
        }
        if (categoria != null && !categoria.isBlank()) {
            lista.removeIf(acc -> !acc.categoria.equalsIgnoreCase(categoria.trim()));
        }

        BigDecimal totalFaturamento = lista.stream().map(a -> a.faturamento).reduce(BigDecimal.ZERO, BigDecimal::add);

        Comparator<Acumulado> ordem = criterio == CriterioRanking.FATURAMENTO
                ? Comparator.comparing((Acumulado a) -> a.faturamento).reversed()
                        .thenComparing(a -> a.quantidade, Comparator.reverseOrder())
                : Comparator.comparing((Acumulado a) -> a.quantidade).reversed()
                        .thenComparing(a -> a.faturamento, Comparator.reverseOrder());
        lista.sort(ordem.thenComparing(a -> Objects.toString(a.nome, "")));

        List<ProdutoRanking> ranking = new ArrayList<>();
        for (int i = 0; i < lista.size() && i < limite; i++) {
            Acumulado a = lista.get(i);
            ranking.add(new ProdutoRanking(i + 1, a.produtoId, Objects.toString(a.nome, "(sem nome)"), a.categoria,
                    a.quantidade, a.faturamento.setScale(2, RoundingMode.HALF_UP),
                    percentual(a.faturamento, totalFaturamento)));
        }
        return ranking;
    }

    // ========================
    // Auxiliares
    // ========================
    public boolean emAndamento(Pedido p) {
        return p.getStatus() != null && !p.getStatus().isFinal();
    }

    public boolean atrasado(Pedido p, LocalDateTime agora, int minutosAtraso) {
        return emAndamento(p) && p.getDataHoraEntrada() != null
                && Duration.between(p.getDataHoraEntrada(), agora).toMinutes() > minutosAtraso;
    }

    /** Variação percentual de "anterior" para "atual"; null quando não há base de comparação. */
    static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (atual == null || anterior == null || anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(CEM).divide(anterior, 1, RoundingMode.HALF_UP);
    }

    static BigDecimal percentual(long parte, long total) {
        return percentual(BigDecimal.valueOf(parte), BigDecimal.valueOf(total));
    }

    static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return parte.multiply(CEM).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static List<Pedido> validos(List<Pedido> pedidos) {
        return pedidos.stream().filter(p -> !p.isCancelado()).toList();
    }

    private static BigDecimal valor(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal subtotalItem(ItemPedido item) {
        if (item.getSubtotal() != null) {
            return item.getSubtotal();
        }
        return item.getPrecoUnitario() == null ? BigDecimal.ZERO
                : item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
    }

    private static FatiaVendas fatia(String chave, String rotulo, List<Pedido> lista, BigDecimal totalPeriodo) {
        BigDecimal fat = lista.stream().map(p -> valor(p.getValorTotal())).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new FatiaVendas(chave, rotulo, lista.size(), fat, percentual(fat, totalPeriodo));
    }

    private static LocalDate inicioDoBalde(LocalDate data, Agrupamento ag) {
        return switch (ag) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
        };
    }

    private static LocalDate proximoBalde(LocalDate inicioBalde, Agrupamento ag) {
        return switch (ag) {
            case DIA -> inicioBalde.plusDays(1);
            case SEMANA -> inicioBalde.plusWeeks(1);
            case MES -> inicioBalde.plusMonths(1);
        };
    }

    private static String rotulo(LocalDate inicioBalde, LocalDate inicioVisivel, Agrupamento ag) {
        return switch (ag) {
            case DIA -> inicioBalde.format(DIA);
            case SEMANA -> "Sem. " + inicioVisivel.format(DIA);
            case MES -> inicioBalde.format(MES);
        };
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** Somatórios básicos de uma lista de pedidos, reaproveitados por vários indicadores. */
    private static final class Totais {
        long total;
        long validos;
        long cancelados;
        long itens;
        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal descontos = BigDecimal.ZERO;

        static Totais de(List<Pedido> pedidos) {
            Totais t = new Totais();
            for (Pedido p : pedidos) {
                t.total++;
                if (p.isCancelado()) {
                    t.cancelados++;
                    continue;
                }
                t.validos++;
                t.itens += p.getQuantidadeItens();
                t.faturamento = t.faturamento.add(valor(p.getValorTotal()));
                t.descontos = t.descontos.add(valor(p.getValorDesconto()));
            }
            t.faturamento = t.faturamento.setScale(2, RoundingMode.HALF_UP);
            t.descontos = t.descontos.setScale(2, RoundingMode.HALF_UP);
            return t;
        }

        BigDecimal ticketMedio() {
            return validos == 0 ? BigDecimal.ZERO.setScale(2)
                    : faturamento.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP);
        }

        BigDecimal taxaCancelamento() {
            return percentual(cancelados, total);
        }
    }

    private static final class Acumulado {
        final String produtoId;
        String nome;
        String categoria;
        long quantidade;
        BigDecimal faturamento = BigDecimal.ZERO;

        Acumulado(String produtoId) {
            this.produtoId = produtoId;
        }
    }
}
