package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.enums.Perfil;
import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class HomeController {

    private static final String LAYOUT = "layout";

    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public HomeController(ClienteService clienteService, UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    // ========================
    // LOGIN E HOME
    // ========================
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(Model model) {
        return tela(model, "home :: content");
    }

    // ========================
    // USUÁRIOS (somente ADMIN)
    // ========================
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return tela(model, "usuarios/lista :: content");
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuarioForm(Model model) {
        return formUsuario(model, new Usuario());
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return usuarioService.buscarPorId(id)
                .map(u -> {
                    // Copia sem a senha: o hash nunca vai para o HTML
                    Usuario form = new Usuario(u.getUsername(), null, u.getRole());
                    form.setId(u.getId());
                    return formUsuario(model, form);
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("erro", "Usuário não encontrado.");
                    return "redirect:/usuarios";
                });
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute("usuario") Usuario usuario, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            usuarioService.criar(new UsuarioRequest(usuario.getUsername(), usuario.getPassword(), usuario.getRole()));
        } catch (RegraNegocioException e) {
            usuario.setId(null);
            usuario.setPassword(null);
            model.addAttribute("erro", e.getMessage());
            return formUsuario(model, usuario);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    public String atualizarUsuario(@PathVariable String id, @ModelAttribute("usuario") Usuario usuario,
                                   Model model, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.atualizar(id,
                    new UsuarioRequest(usuario.getUsername(), usuario.getPassword(), usuario.getRole()));
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            usuario.setId(id);
            usuario.setPassword(null);
            model.addAttribute("erro", e.getMessage());
            return formUsuario(model, usuario);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/deletar/{id}")
    public String deletarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.deletarUsuario(id);
            redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    private String formUsuario(Model model, Usuario usuario) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfis", Perfil.values());
        return tela(model, "usuarios/form :: content");
    }

    // ========================
    // CLIENTES
    // ========================
    @GetMapping("/clientes")
    public String clientes(Model model) {
        // A tabela é carregada via JavaScript (GET /api/clientes)
        return tela(model, "clientes/lista :: content");
    }

    @GetMapping("/clientes/novo")
    public String novoClienteForm(Model model) {
        return formCliente(model, new Cliente());
    }

    @PostMapping("/clientes/salvar")
    public String salvarCliente(@ModelAttribute("cliente") Cliente cliente, Model model,
                                RedirectAttributes redirectAttributes) {
        cliente.setId(null); // inclusão: ignora qualquer id enviado no formulário
        try {
            clienteService.salvar(cliente);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return formCliente(model, cliente);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return clienteService.buscarPorId(id)
                .map(c -> formCliente(model, c))
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("erro", "Cliente não encontrado.");
                    return "redirect:/clientes";
                });
    }

    @PostMapping("/clientes/editar/{id}")
    public String atualizarCliente(@PathVariable String id, @ModelAttribute("cliente") Cliente cliente,
                                   Model model, RedirectAttributes redirectAttributes) {
        cliente.setId(id);
        try {
            clienteService.salvar(cliente);
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            model.addAttribute("erro", e.getMessage());
            return formCliente(model, cliente);
        }
        redirectAttributes.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    private String formCliente(Model model, Cliente cliente) {
        model.addAttribute("cliente", cliente);
        return tela(model, "clientes/form :: content");
    }

    // ========================
    // PEDIDOS (telas consomem /api/pedidos)
    // ========================
    @GetMapping("/pedidos")
    public String pedidos(Model model) {
        model.addAttribute("todosStatus", StatusPedido.values()); // opções do filtro
        return tela(model, "pedidos/lista :: content");
    }

    @GetMapping("/pedidos/novo")
    public String novoPedido(Model model) {
        model.addAttribute("tiposAtendimento", TipoAtendimento.values());
        return tela(model, "pedidos/form :: content");
    }

    @GetMapping("/pedidos/{id}")
    public String detalhePedido(@PathVariable String id, Model model) {
        model.addAttribute("pedidoId", id); // o JavaScript da página usa para chamar a API
        return tela(model, "pedidos/detalhe :: content");
    }

    // ========================
    // PRODUTOS (telas consomem /api/produtos)
    // ========================
    @GetMapping("/produtos")
    public String produtos(Model model) {
        return tela(model, "produtos/lista :: content");
    }

    @GetMapping("/produtos/novo")
    public String novoProduto(Model model) {
        return tela(model, "produtos/form :: content");
    }

    @GetMapping("/produtos/editar/{id}")
    public String editarProduto(@PathVariable String id, Model model) {
        model.addAttribute("produtoId", id);
        return tela(model, "produtos/form :: content");
    }

    // ========================
    // AUDITORIA (tela consome /api/auditoria, somente leitura)
    // ========================
    @GetMapping("/auditoria")
    public String auditoria(Model model) {
        model.addAttribute("operacoes", TipoOperacao.values()); // opções do filtro
        return tela(model, "auditoria/lista :: content");
    }

    // ========================
    // OUTRAS ROTAS
    // ========================
    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        return tela(model, "configuracoes :: content");
    }

    /** Monta a página: o layout.html encaixa o fragmento informado no conteúdo principal. */
    private String tela(Model model, String fragmento) {
        model.addAttribute("content", fragmento);
        return LAYOUT;
    }
}