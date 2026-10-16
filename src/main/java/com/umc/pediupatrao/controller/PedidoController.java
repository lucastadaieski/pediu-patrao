package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AcoesPedidoResponse;
import com.umc.pediupatrao.dto.AcoesPedidoResponse.OpcaoStatus;
import com.umc.pediupatrao.dto.CancelamentoRequest;
import com.umc.pediupatrao.dto.DescontoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de pedidos. O controller só recebe, valida o formato (@Valid) e repassa;
 * as regras ficam no PedidoService.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@Valid @RequestBody PedidoRequest pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(pedido));
    }

    @GetMapping
    public List<Pedido> listarPedidos(@RequestParam(required = false) StatusPedido status) {
        return pedidoService.listarPorStatus(status);
    }

    @GetMapping("/{id}")
    public Pedido buscarPorId(@PathVariable String id) {
        return pedidoService.buscarPorId(id);
    }

    /** Ações disponíveis agora (próximos status, se pode cancelar ou dar desconto). */
    @GetMapping("/{id}/acoes")
    public AcoesPedidoResponse acoes(@PathVariable String id) {
        Pedido pedido = pedidoService.buscarPorId(id);
        List<OpcaoStatus> proximos = pedidoService.proximosStatusDisponiveis(pedido).stream()
                .map(s -> new OpcaoStatus(s, s.getDescricao()))
                .toList();
        return new AcoesPedidoResponse(proximos, pedidoService.podeCancelar(pedido),
                pedidoService.podeAplicarDesconto(pedido), PedidoService.DESCONTO_MAXIMO);
    }

    /** Altera itens/tipo/observação enquanto o pedido está RECEBIDO. */
    @PutMapping("/{id}")
    public Pedido atualizarItens(@PathVariable String id, @Valid @RequestBody PedidoRequest pedido) {
        return pedidoService.atualizarItens(id, pedido);
    }

    /** Mesma rota da versão original: PUT /api/pedidos/{id}/status?status=EM_PREPARACAO */
    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, status);
    }

    /** Exclusivo do GERENTE. */
    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable String id, @Valid @RequestBody CancelamentoRequest dados) {
        return pedidoService.cancelar(id, dados.justificativa());
    }

    /** Exclusivo do GERENTE. Percentual de 0 a 20. */
    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @Valid @RequestBody DescontoRequest dados) {
        return pedidoService.aplicarDesconto(id, dados.percentual(), dados.justificativa());
    }
}
