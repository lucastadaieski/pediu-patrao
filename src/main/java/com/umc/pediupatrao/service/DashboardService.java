package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FatiaVendas;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro.Opcao;
import com.umc.pediupatrao.dto.dashboard.PedidoRecente;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Busca os pedidos no MongoDB conforme o filtro e repassa para a DashboardCalculadora.
 *
 * O filtro (período, status, tipo de atendimento) é aplicado NO BANCO, usando o índice de
 * "dataHoraEntrada" criado no MongoInitConfig. Só os pedidos do período vêm para a memória.
 */
@Service
public class DashboardService {

    /** Limites dos parâmetros "limite" (ranking e últimos pedidos). */
    public static final int LIMITE_MAXIMO = 50;

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;
    private final DashboardCalculadora calculadora;
    private final int minutosAtraso;
    private final Clock relogio;

    @Autowired
    public DashboardService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository,
                            @Value("${app.dashboard.minutos-atraso:40}") int minutosAtraso) {
        this(mongoTemplate, produtoRepository, minutosAtraso, Clock.systemDefaultZone());
    }

    /** Construtor usado nos testes, com relógio fixo. */
    DashboardService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository, int minutosAtraso,
                     Clock relogio) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
        this.calculadora = new DashboardCalculadora();
        this.minutosAtraso = minutosAtraso;
        this.relogio = relogio;
    }

    /** Monta o filtro aplicando os padrões de período (usado pelo controller). */
    public FiltroDashboard filtro(LocalDate inicio, LocalDate fim, StatusPedido status, TipoAtendimento tipo) {
        return FiltroDashboard.de(inicio, fim, status, tipo, LocalDate.now(relogio));
    }

    public ResumoVendas resumo(FiltroDashboard filtro) {
        FiltroDashboard anterior = filtro.periodoAnterior();
        return calculadora.resumo(filtro, buscar(filtro), anterior, buscar(anterior), agora());
    }

    public List<PontoSerie> serie(FiltroDashboard filtro, Agrupamento agrupamento) {
        return calculadora.serie(filtro, buscar(filtro), agrupamento);
    }

    public List<FatiaVendas> porHora(FiltroDashboard filtro) {
        return calculadora.porHora(buscar(filtro));
    }

    public List<FatiaVendas> porDiaSemana(FiltroDashboard filtro) {
        return calculadora.porDiaSemana(buscar(filtro));
    }

    public List<FatiaVendas> porTipoAtendimento(FiltroDashboard filtro) {
        return calculadora.porTipoAtendimento(buscar(filtro));
    }

    public SituacaoPedidos situacao(FiltroDashboard filtro) {
        return calculadora.situacao(buscar(filtro), agora(), minutosAtraso);
    }

    public List<PedidoRecente> recentes(FiltroDashboard filtro, int limite) {
        return calculadora.recentes(buscar(filtro), validarLimite(limite), agora(), minutosAtraso);
    }

    public List<ProdutoRanking> ranking(FiltroDashboard filtro, CriterioRanking criterio, int limite,
                                        String categoria) {
        Map<String, String> categorias = new HashMap<>();
        for (Produto produto : produtoRepository.findAll()) {
            if (produto.getId() != null) {
                categorias.put(produto.getId(), produto.getTipo());
            }
        }
        CriterioRanking c = criterio == null ? CriterioRanking.QUANTIDADE : criterio;
        return calculadora.ranking(buscar(filtro), categorias, c, validarLimite(limite), categoria);
    }

    public OpcoesFiltro opcoes() {
        FiltroDashboard padrao = filtro(null, null, null, null);
        List<String> categorias = produtoRepository.findAll().stream()
                .map(Produto::getTipo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new OpcoesFiltro(padrao.inicio(), padrao.fim(), FiltroDashboard.DIAS_MAXIMO,
                Arrays.stream(StatusPedido.values()).map(s -> new Opcao(s.name(), s.getDescricao())).toList(),
                Arrays.stream(TipoAtendimento.values()).map(t -> new Opcao(t.name(), t.getDescricao())).toList(),
                Arrays.stream(Agrupamento.values()).map(a -> new Opcao(a.name(), a.getDescricao())).toList(),
                Arrays.stream(CriterioRanking.values()).map(c -> new Opcao(c.name(), c.getDescricao())).toList(),
                categorias);
    }

    /** Consulta no MongoDB só os pedidos do período e dos filtros informados. */
    List<Pedido> buscar(FiltroDashboard filtro) {
        Criteria criterio = Criteria.where("dataHoraEntrada")
                .gte(filtro.inicioDataHora())
                .lt(filtro.fimExclusivo());
        if (filtro.status() != null) {
            criterio = criterio.and("status").is(filtro.status().name());
        }
        if (filtro.tipoAtendimento() != null) {
            criterio = criterio.and("tipoAtendimento").is(filtro.tipoAtendimento().name());
        }
        return mongoTemplate.find(Query.query(criterio), Pedido.class);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private static int validarLimite(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }
        return limite;
    }
}
