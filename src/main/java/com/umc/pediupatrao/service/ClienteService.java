package com.umc.pediupatrao.service;

import static com.umc.pediupatrao.service.AuditoriaService.campo;
import static com.umc.pediupatrao.service.AuditoriaService.compararCampos;
import static com.umc.pediupatrao.service.AuditoriaService.retrato;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;


@Service
public class ClienteService {

    private static final String ENTIDADE = "Cliente";

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository,
                          AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    /** Mantido por compatibilidade: cria um cliente novo. */
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente) {
        cliente.setId(null);
        return salvar(cliente);
    }

    /** Sem id = cria; com id = atualiza (comportamento original preservado). */
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {
        validar(cliente);
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            cliente.setId(null);
            Cliente salvo = clienteRepository.save(cliente);
            // Minimização de dados: na inclusão registra só o nome, não o endereço completo
            auditoriaService.registrar(TipoOperacao.CRIACAO, ENTIDADE, salvo.getId(),
                    List.of(campo("nome", null, salvo.getNome())), null);
            return salvo;
        }

        Cliente anterior = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para atualização."));

        List<AlteracaoCampo> alteracoes = compararCampos(retratoDe(anterior), retratoDe(cliente));
        Cliente salvo = clienteRepository.save(cliente);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO, ENTIDADE, salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        if (pedidoRepository.existsByClienteId(id)) {
            throw new RegraNegocioException(
                    "Este cliente possui pedidos registrados. A exclusão foi bloqueada para preservar o histórico.");
        }
        clienteRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO, ENTIDADE, id,
                List.of(campo("nome", cliente.getNome(), null)), null);
    }

    private void validar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            throw new RegraNegocioException("O telefone do cliente é obrigatório.");
        }
    }

    /** Campos acompanhados pela auditoria (com destaque para telefone e endereço). */
    private static Map<String, Object> retratoDe(Cliente c) {
        return retrato(
                "nome", c.getNome(),
                "telefone", c.getTelefone(),
                "cep", c.getCep(),
                "logradouro", c.getLogradouro(),
                "numero", c.getNumero(),
                "complemento", c.getComplemento(),
                "bairro", c.getBairro(),
                "cidade", c.getCidade(),
                "estado", c.getEstado(),
                "endereco", c.getEndereco());
    }
}
