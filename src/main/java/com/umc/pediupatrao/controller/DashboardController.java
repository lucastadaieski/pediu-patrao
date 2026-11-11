package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FatiaVendas;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.PedidoRecente;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API do Dashboard Administrativo. Somente leitura (só GET) e somente ADMIN e GERENTE.
 *
 * Todos os endpoints aceitam os mesmos filtros (RF05), todos opcionais:
 *   inicio, fim       datas no formato AAAA-MM-DD (padrão: últimos 30 dias)
 *   status            RECEBIDO, EM_PREPARACAO, ..., CANCELADO
 *   tipoAtendimento   ENTREGA ou RETIRADA
 *
 * Exemplo: GET /api/dashboard/resumo?inicio=2026-11-01&fim=2026-11-10&tipoAtendimento=ENTREGA
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')") // reforço; a regra principal está no SecurityConfig
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** RF01: faturamento, pedidos, ticket médio, cancelamentos e comparação com o período anterior. */
    @GetMapping("/resumo")
    public ResumoVendas resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return dashboardService.resumo(filtro(inicio, fim, status, tipoAtendimento));
    }

    /** RF02: vendas ao longo do tempo, agrupadas por DIA, SEMANA ou MES. */
    @GetMapping("/vendas/serie")
    public List<PontoSerie> serie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(defaultValue = "DIA") Agrupamento agrupamento) {
        return dashboardService.serie(filtro(inicio, fim, status, tipoAtendimento), agrupamento);
    }

    /** RF02: pedidos e faturamento por hora do dia (0h a 23h), para achar o horário de pico. */
    @GetMapping("/vendas/por-hora")
    public List<FatiaVendas> porHora(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return dashboardService.porHora(filtro(inicio, fim, status, tipoAtendimento));
    }

    /** RF02: pedidos e faturamento por dia da semana (segunda a domingo). */
    @GetMapping("/vendas/por-dia-semana")
    public List<FatiaVendas> porDiaSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return dashboardService.porDiaSemana(filtro(inicio, fim, status, tipoAtendimento));
    }

    /** RF02: entrega x retirada. */
    @GetMapping("/vendas/por-tipo-atendimento")
    public List<FatiaVendas> porTipoAtendimento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return dashboardService.porTipoAtendimento(filtro(inicio, fim, status, tipoAtendimento));
    }

    /** RF03: quantidade por status, em andamento, atrasados e tempo médio de atendimento. */
    @GetMapping("/pedidos/situacao")
    public SituacaoPedidos situacao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento) {
        return dashboardService.situacao(filtro(inicio, fim, status, tipoAtendimento));
    }

    /** RF03: últimos pedidos do período (mais recentes primeiro). */
    @GetMapping("/pedidos/recentes")
    public List<PedidoRecente> recentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(defaultValue = "10") int limite) {
        return dashboardService.recentes(filtro(inicio, fim, status, tipoAtendimento), limite);
    }

    /** RF04: produtos mais vendidos, por QUANTIDADE ou FATURAMENTO, opcionalmente de uma categoria. */
    @GetMapping("/produtos/ranking")
    public List<ProdutoRanking> ranking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoAtendimento tipoAtendimento,
            @RequestParam(defaultValue = "QUANTIDADE") CriterioRanking criterio,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(required = false) String categoria) {
        return dashboardService.ranking(filtro(inicio, fim, status, tipoAtendimento), criterio, limite, categoria);
    }

    /** RF05: opções válidas para os filtros da tela (status, tipos, agrupamentos, categorias). */
    @GetMapping("/filtros")
    public OpcoesFiltro filtros() {
        return dashboardService.opcoes();
    }

    private FiltroDashboard filtro(LocalDate inicio, LocalDate fim, StatusPedido status, TipoAtendimento tipo) {
        return dashboardService.filtro(inicio, fim, status, tipo);
    }
}
