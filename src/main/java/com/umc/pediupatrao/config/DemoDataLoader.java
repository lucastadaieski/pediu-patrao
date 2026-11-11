package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga OPCIONAL de pedidos de demonstração para o dashboard não aparecer vazio.
 *
 * Só roda com app.demo.carregar=true (no .env: APP_DEMO_CARREGAR=true) e só uma vez:
 * se já existir algum pedido com responsável "demo", não faz nada.
 *
 * Os pedidos são gravados direto no repositório (sem passar pelo PedidoService), por isso
 * NÃO geram auditoria. Para apagar depois, no MongoDB:
 *     db.pedidos.deleteMany({ responsavelEntrada: "demo" })
 */
@Component
@Order(100) // depois do DataInitializer e do MongoInitConfig
@ConditionalOnProperty(name = "app.demo.carregar", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    public static final String RESPONSAVEL_DEMO = "demo";
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);
    private static final BigDecimal CEM = new BigDecimal("100");

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final int dias;

    public DemoDataLoader(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
                          ClienteRepository clienteRepository, @Value("${app.demo.dias:60}") int dias) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.dias = dias;
    }

    @Override
    public void run(String... args) {
        if (pedidoRepository.existsByResponsavelEntrada(RESPONSAVEL_DEMO)) {
            log.info("Dados de demonstração já existem: nada a fazer.");
            return;
        }
        List<Produto> produtos = produtoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()) && p.getPreco() != null && p.getPreco() > 0)
                .toList();
        if (produtos.isEmpty()) {
            produtos = produtoRepository.saveAll(cardapioDemo());
            log.info("Nenhum produto ativo: {} produtos de demonstração cadastrados.", produtos.size());
        }
        List<Cliente> clientes = clienteRepository.findAll();
        if (clientes.isEmpty()) {
            clientes = clienteRepository.saveAll(clientesDemo());
            log.info("Nenhum cliente: {} clientes de demonstração cadastrados.", clientes.size());
        }

        List<Pedido> pedidos = gerar(produtos, clientes, LocalDateTime.now(), dias, new Random(42));
        pedidoRepository.saveAll(pedidos);
        log.info("{} pedidos de demonstração criados nos últimos {} dias.", pedidos.size(), dias);
    }

    /**
     * Gera os pedidos em memória (sem banco), para poder ser testado.
     * Mais pedidos de sexta a domingo e nos horários de almoço e jantar, como numa pizzaria real.
     */
    static List<Pedido> gerar(List<Produto> produtos, List<Cliente> clientes, LocalDateTime agora, int dias,
                              Random rnd) {
        List<Pedido> pedidos = new ArrayList<>();
        LocalDate hoje = agora.toLocalDate();
        for (int d = dias - 1; d >= 0; d--) {
            LocalDate dia = hoje.minusDays(d);
            DayOfWeek semana = dia.getDayOfWeek();
            int base = switch (semana) {
                case FRIDAY, SATURDAY -> 10;
                case SUNDAY -> 8;
                case MONDAY -> 3;
                default -> 5;
            };
            int quantidade = base + rnd.nextInt(5);
            for (int i = 0; i < quantidade; i++) {
                LocalDateTime entrada = dia.atTime(horaDoPedido(rnd), rnd.nextInt(60));
                if (entrada.isAfter(agora)) {
                    continue; // não cria pedido "no futuro" no dia de hoje
                }
                Cliente cliente = clientes.get(rnd.nextInt(clientes.size()));
                pedidos.add(pedido(produtos, cliente, entrada, agora, rnd));
            }
        }
        return pedidos;
    }

    private static int horaDoPedido(Random rnd) {
        int sorteio = rnd.nextInt(100);
        if (sorteio < 25) {
            return 11 + rnd.nextInt(3);   // almoço: 11h a 13h
        }
        if (sorteio < 35) {
            return 14 + rnd.nextInt(4);   // tarde: 14h a 17h
        }
        return 18 + rnd.nextInt(5);       // jantar: 18h a 22h (pico)
    }

    private static Pedido pedido(List<Produto> produtos, Cliente cliente, LocalDateTime entrada,
                                 LocalDateTime agora, Random rnd) {
        Pedido p = new Pedido();
        p.setClienteId(cliente.getId());
        p.setClienteNome(cliente.getNome());
        p.setTipoAtendimento(rnd.nextInt(100) < 60 ? TipoAtendimento.ENTREGA : TipoAtendimento.RETIRADA);
        p.setDataHoraEntrada(entrada);
        p.setResponsavelEntrada(RESPONSAVEL_DEMO);

        List<ItemPedido> itens = new ArrayList<>();
        int qtdItens = 1 + rnd.nextInt(3);
        for (int i = 0; i < qtdItens; i++) {
            Produto produto = produtos.get(rnd.nextInt(produtos.size()));
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), preco, 1 + rnd.nextInt(2)));
        }
        p.setItens(itens);

        // Desconto em ~8% dos pedidos (5% ou 10%), dentro do limite de 20% do sistema
        BigDecimal percentual = rnd.nextInt(100) < 8 ? BigDecimal.valueOf(rnd.nextBoolean() ? 5 : 10) : BigDecimal.ZERO;
        BigDecimal subtotal = itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal desconto = subtotal.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);
        p.setSubtotal(subtotal);
        p.setPercentualDesconto(percentual.setScale(2, RoundingMode.HALF_UP));
        p.setValorDesconto(desconto);
        p.setValorTotal(subtotal.subtract(desconto));
        if (percentual.signum() > 0) {
            p.setDataHoraDesconto(entrada.plusMinutes(2));
            p.setResponsavelDesconto(RESPONSAVEL_DEMO);
        }

        definirStatus(p, entrada, agora, rnd);
        return p;
    }

    /** Pedidos antigos terminam (finalizados ou cancelados); os de agora ficam no meio do fluxo. */
    private static void definirStatus(Pedido p, LocalDateTime entrada, LocalDateTime agora, Random rnd) {
        long minutos = java.time.Duration.between(entrada, agora).toMinutes();
        boolean cancelar = rnd.nextInt(100) < 7;

        if (cancelar && minutos > 15) {
            p.setStatus(StatusPedido.CANCELADO);
            p.setDataHoraCancelamento(entrada.plusMinutes(5 + rnd.nextInt(10)));
            p.setResponsavelCancelamento(RESPONSAVEL_DEMO);
            p.setJustificativaCancelamento("Cancelamento de demonstração");
            return;
        }
        int preparo = 20 + rnd.nextInt(20);   // entrada -> saída
        int entrega = 10 + rnd.nextInt(25);   // saída -> finalização
        if (minutos >= preparo + entrega) {
            registrarSaida(p, entrada.plusMinutes(preparo));
            p.setStatus(StatusPedido.FINALIZADO);
            p.setDataHoraFinalizacao(entrada.plusMinutes(preparo + entrega));
            p.setResponsavelFinalizacao(RESPONSAVEL_DEMO);
        } else if (minutos >= preparo) {
            registrarSaida(p, entrada.plusMinutes(preparo));
            p.setStatus(p.getTipoAtendimento().statusDeSaida());
        } else if (minutos >= preparo * 2 / 3) {
            p.setStatus(StatusPedido.PRONTO);
        } else if (minutos >= 5) {
            p.setStatus(StatusPedido.EM_PREPARACAO);
        } else {
            p.setStatus(StatusPedido.RECEBIDO);
        }
    }

    private static void registrarSaida(Pedido p, LocalDateTime saida) {
        p.setDataHoraSaida(saida);
        p.setResponsavelSaida(RESPONSAVEL_DEMO);
    }

    private static List<Produto> cardapioDemo() {
        Object[][] dados = {
                {"Pizza Calabresa", "Pizza", 49.90}, {"Pizza Margherita", "Pizza", 52.90},
                {"Pizza Portuguesa", "Pizza", 56.90}, {"Pizza Frango com Catupiry", "Pizza", 58.90},
                {"Esfiha de Carne", "Esfiha", 6.50}, {"Esfiha de Queijo", "Esfiha", 6.50},
                {"X-Burguer", "Lanche", 24.90}, {"Refrigerante 2L", "Bebida", 14.00},
                {"Suco Natural 500ml", "Bebida", 9.90}};
        List<Produto> lista = new ArrayList<>();
        for (Object[] d : dados) {
            Produto p = new Produto();
            p.setNome((String) d[0]);
            p.setTipo((String) d[1]);
            p.setPreco((Double) d[2]);
            p.setAtivo(true);
            lista.add(p);
        }
        return lista;
    }

    private static List<Cliente> clientesDemo() {
        String[] nomes = {"Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Alves", "Elaine Costa", "Fábio Rocha"};
        List<Cliente> lista = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            Cliente c = new Cliente();
            c.setNome(nomes[i]);
            c.setTelefone("1199999000" + i);
            c.setCidade("Mogi das Cruzes");
            c.setEstado("SP");
            lista.add(c);
        }
        return lista;
    }
}
