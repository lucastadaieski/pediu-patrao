package com.umc.pediupatrao.service;

import static com.umc.pediupatrao.service.AuditoriaService.campo;
import static com.umc.pediupatrao.service.AuditoriaService.compararCampos;
import static com.umc.pediupatrao.service.AuditoriaService.retrato;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private static final String ENTIDADE = "Produto";

    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public ProdutoService(ProdutoRepository produtoRepository, AuditoriaService auditoriaService) {
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(String id) {
        return produtoRepository.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto novoProduto(Produto produto) {
        produto.setId(null);
        return salvar(produto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(Produto produto) {
        validar(produto);
        if (produto.getId() == null || produto.getId().isBlank()) {
            produto.setId(null);
            Produto salvo = produtoRepository.save(produto);
            auditoriaService.registrar(TipoOperacao.CRIACAO, ENTIDADE, salvo.getId(),
                    List.of(campo("nome", null, salvo.getNome()), campo("preco", null, salvo.getPreco())), null);
            return salvo;
        }
        Produto anterior = produtoRepository.findById(produto.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado para atualização."));
        List<AlteracaoCampo> alteracoes = compararCampos(retratoDe(anterior), retratoDe(produto));
        Produto salvo = produtoRepository.save(produto);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO, ENTIDADE, salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        // Os pedidos guardam nome e preço do item, então excluir o produto não altera o histórico
        produtoRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO, ENTIDADE, id,
                List.of(campo("nome", produto.getNome(), null)), null);
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraNegocioException("O preço do produto deve ser maior que zero.");
        }
        if (produto.getAtivo() == null) {
            produto.setAtivo(Boolean.FALSE);
        }
    }

    private static Map<String, Object> retratoDe(Produto p) {
        return retrato("nome", p.getNome(), "tipo", p.getTipo(), "preco", p.getPreco(), "ativo", p.getAtivo());
    }
}
