package com.umc.pediupatrao.service;

import static com.umc.pediupatrao.service.AuditoriaService.campo;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.UsuarioAtual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;


@Service
public class PedidoService {

    /** Limite do desconto: 20% do valor do pedido (RF04). */
    public static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final String ENTIDADE = "Pedido";

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioAtual usuarioAtual;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
                         ClienteRepository clienteRepository, AuditoriaService auditoriaService,
                         UsuarioAtual usuarioAtual) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioAtual = usuarioAtual;
    }

    // =====================================================================
    // Consultas
    // =====================================================================

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByDataHoraEntradaDesc();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPorStatus(StatusPedido status) {
        return status == null ? listarPedidos() : pedidoRepository.findByStatusOrderByDataHoraEntradaDesc(status);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    // =====================================================================
    // RF02: registro do pedido com itens e valores calculados no servidor
    // =====================================================================

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido criarPedido(PedidoRequest dados) {
        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoAtendimento(dados.tipoAtendimento());
        pedido.setObservacao(limpar(dados.observacao()));
        pedido.setItens(montarItens(dados.itens()));
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        recalcularValores(pedido);

        // RF03 passo 1: RECEBIDO, com data/hora de entrada e responsável
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelEntrada(usuarioAtual.username());

        Pedido salvo = pedidoRepository.save(pedido);

        auditoriaService.registrar(TipoOperacao.CRIACAO, ENTIDADE, salvo.getId(), List.of(
                campo("cliente", null, salvo.getClienteNome()),
                campo("tipoAtendimento", null, salvo.getTipoAtendimento()),
                campo("itens", null, resumoItens(salvo.getItens())),
                campo("valorTotal", null, salvo.getValorTotal()),
                campo("status", null, salvo.getStatus())), null);
        return salvo;
    }

    /** Alteração dos itens: só enquanto o pedido está RECEBIDO (ainda não foi para a cozinha). */
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarItens(String id, PedidoRequest dados) {
        Pedido pedido = buscarPorId(id);
        if (pedido.getStatus() != StatusPedido.RECEBIDO) {
            throw new RegraNegocioException("Os itens só podem ser alterados enquanto o pedido está RECEBIDO.");
        }
        if (!pedido.getClienteId().equals(dados.clienteId())) {
            throw new RegraNegocioException("Não é permitido trocar o cliente de um pedido já registrado.");
        }

        String itensAntes = resumoItens(pedido.getItens());
        BigDecimal totalAntes = pedido.getValorTotal();
        var tipoAntes = pedido.getTipoAtendimento();
        String obsAntes = pedido.getObservacao();

        pedido.setItens(montarItens(dados.itens()));
        pedido.setTipoAtendimento(dados.tipoAtendimento());
        pedido.setObservacao(limpar(dados.observacao()));
        recalcularValores(pedido); // mantém o percentual de desconto já autorizado

        Pedido salvo = pedidoRepository.save(pedido);

        List<AlteracaoCampo> alteracoes = AuditoriaService.compararCampos(
                AuditoriaService.retrato("itens", itensAntes, "tipoAtendimento", tipoAntes,
                        "observacao", obsAntes, "valorTotal", totalAntes),
                AuditoriaService.retrato("itens", resumoItens(salvo.getItens()),
                        "tipoAtendimento", salvo.getTipoAtendimento(),
                        "observacao", salvo.getObservacao(), "valorTotal", salvo.getValorTotal()));
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO, ENTIDADE, id, alteracoes, null);
        }
        return salvo;
    }

    // =====================================================================
    // RF03: transições de status
    // =====================================================================

    /** Versão que recebe texto (mantém a assinatura da API original: ?status=...). */
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarStatus(String id, String status) {
        return atualizarStatus(id, converterStatus(status));
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatus();

        if (novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException(
                    "Para cancelar use a operação de cancelamento (exclusiva do GERENTE, com justificativa).");
        }
        if (atual == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Pedido cancelado não pode ter o status alterado.");
        }
        if (atual == StatusPedido.FINALIZADO) {
            throw new RegraNegocioException("Pedido finalizado não pode ter o status alterado.");
        }
        if (!atual.podeIrPara(novoStatus)) {
            throw new RegraNegocioException("Transição inválida: " + atual + " → " + novoStatus
                    + ". A partir de " + atual + " é permitido: " + descreverProximos(pedido) + ".");
        }
        if (novoStatus.isSaida() && novoStatus != pedido.getTipoAtendimento().statusDeSaida()) {
            throw new RegraNegocioException("Pedido de " + pedido.getTipoAtendimento().getDescricao().toLowerCase()
                    + " deve sair como " + pedido.getTipoAtendimento().statusDeSaida() + ".");
        }

        LocalDateTime agora = LocalDateTime.now();
        String responsavel = usuarioAtual.username();
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        alteracoes.add(campo("status", atual, novoStatus));

        pedido.setStatus(novoStatus);

        // RF03 passo 4: a saída exige data/hora e responsável
        if (novoStatus.isSaida()) {
            pedido.setDataHoraSaida(agora);
            pedido.setResponsavelSaida(responsavel);
            alteracoes.add(campo("dataHoraSaida", null, agora));
            alteracoes.add(campo("responsavelSaida", null, responsavel));
        }
        if (novoStatus == StatusPedido.FINALIZADO) {
            pedido.setDataHoraFinalizacao(agora);
            pedido.setResponsavelFinalizacao(responsavel);
        }

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.MUDANCA_STATUS, ENTIDADE, id, alteracoes, null);
        return salvo;
    }

    /** Próximos status que a tela pode oferecer (sem CANCELADO, que é operação própria). */
    public List<StatusPedido> proximosStatusDisponiveis(Pedido pedido) {
        StatusPedido saida = pedido.getTipoAtendimento() == null ? null : pedido.getTipoAtendimento().statusDeSaida();
        return pedido.getStatus().proximosPermitidos().stream()
                .filter(s -> s != StatusPedido.CANCELADO)
                .filter(s -> !s.isSaida() || s == saida)
                .sorted()
                .toList();
    }

    // =====================================================================
    // RF04: cancelamento e desconto (exclusivos do GERENTE)
    // =====================================================================

    public boolean podeCancelar(Pedido pedido) {
        return !pedido.getStatus().isFinal() && !pedido.getStatus().jaSaiu();
    }

    public boolean podeAplicarDesconto(Pedido pedido) {
        return podeCancelar(pedido);
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        if (justificativa == null || justificativa.trim().length() < 10) {
            throw new RegraNegocioException("A justificativa do cancelamento é obrigatória (mínimo de 10 caracteres).");
        }
        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatus();

        if (atual == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O pedido já está cancelado.");
        }
        if (atual.jaSaiu()) {
            throw new RegraNegocioException("Não é possível cancelar: o pedido já saiu (status " + atual + ").");
        }

        LocalDateTime agora = LocalDateTime.now();
        String responsavel = usuarioAtual.username();

        // Os demais dados (itens, valores, entrada) são preservados como estavam: histórico intacto
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setDataHoraCancelamento(agora);
        pedido.setResponsavelCancelamento(responsavel);
        pedido.setJustificativaCancelamento(justificativa.trim());

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO, ENTIDADE, id, List.of(
                campo("status", atual, StatusPedido.CANCELADO)),
                justificativa.trim());
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, String justificativa) {
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O percentual de desconto deve ser maior que zero.");
        }
        if (percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new RegraNegocioException("O desconto é limitado a " + DESCONTO_MAXIMO + "% do valor do pedido.");
        }
        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatus();
        if (atual == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Não é possível aplicar desconto em pedido cancelado.");
        }
        if (atual.jaSaiu()) {
            throw new RegraNegocioException(
                    "O desconto só pode ser aplicado antes da saída para entrega ou retirada (status atual " + atual + ").");
        }

        BigDecimal percentualAntes = pedido.getPercentualDesconto();
        BigDecimal descontoAntes = pedido.getValorDesconto();
        BigDecimal totalAntes = pedido.getValorTotal();

        pedido.setPercentualDesconto(percentual.setScale(2, RoundingMode.HALF_UP));
        pedido.setDataHoraDesconto(LocalDateTime.now());
        pedido.setResponsavelDesconto(usuarioAtual.username());
        recalcularValores(pedido);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.DESCONTO, ENTIDADE, id, List.of(
                campo("percentualDesconto", percentualAntes, salvo.getPercentualDesconto()),
                campo("valorDesconto", descontoAntes, salvo.getValorDesconto()),
                campo("valorTotal", totalAntes, salvo.getValorTotal())),
                limpar(justificativa));
        return salvo;
    }

    // =====================================================================
    // Cálculos (sempre no servidor)
    // =====================================================================

    /**
     * Busca cada produto no banco e copia nome e preço. O preço NUNCA vem da requisição.
     * Itens repetidos do mesmo produto são somados numa linha só.
     */
    List<ItemPedido> montarItens(List<ItemPedidoRequest> itensSolicitados) {
        if (itensSolicitados == null || itensSolicitados.isEmpty()) {
            throw new RegraNegocioException("O pedido precisa de ao menos um item.");
        }
        var quantidadesPorProduto = itensSolicitados.stream()
                .collect(Collectors.groupingBy(ItemPedidoRequest::produtoId,
                        java.util.LinkedHashMap::new,
                        Collectors.summingInt(ItemPedidoRequest::quantidade)));

        List<ItemPedido> itens = new ArrayList<>();
        quantidadesPorProduto.forEach((produtoId, quantidade) -> {
            if (quantidade < 1) {
                throw new RegraNegocioException("A quantidade de cada item deve ser ao menos 1.");
            }
            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new RegraNegocioException("Produto não encontrado: " + produtoId));
            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' está inativo.");
            }
            if (produto.getPreco() == null || produto.getPreco() <= 0) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' está sem preço válido.");
            }
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), preco, quantidade));
        });
        return itens;
    }

    /** subtotal = soma dos itens; desconto = subtotal x percentual; total = subtotal - desconto. */
    void recalcularValores(Pedido pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentual = pedido.getPercentualDesconto() == null ? BigDecimal.ZERO : pedido.getPercentualDesconto();
        BigDecimal desconto = subtotal.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);

        pedido.setSubtotal(subtotal);
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(subtotal.subtract(desconto));
    }

    private StatusPedido converterStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new RegraNegocioException("Informe o novo status.");
        }
        try {
            return StatusPedido.valueOf(status.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Status inválido: " + status);
        }
    }

    private String descreverProximos(Pedido pedido) {
        List<StatusPedido> proximos = proximosStatusDisponiveis(pedido);
        return proximos.isEmpty() ? "nenhuma transição" : proximos.toString();
    }

    private static String resumoItens(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::toString).collect(Collectors.joining(", "));
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
