package com.umc.pediupatrao.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.dto.ErroResponse;
import com.umc.pediupatrao.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
public class AcessoNegadoHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AcessoNegadoHandler.class);

    private final AuditoriaService auditoriaService;
    private final UsuarioAtual usuarioAtual;
    private final ObjectMapper objectMapper;

    public AcessoNegadoHandler(AuditoriaService auditoriaService, UsuarioAtual usuarioAtual,
                               ObjectMapper objectMapper) {
        this.auditoriaService = auditoriaService;
        this.usuarioAtual = usuarioAtual;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        String rota = request.getMethod() + " " + request.getRequestURI();
        String motivo = exception instanceof CsrfException
                ? "Token CSRF ausente ou inválido"
                : "Perfil sem permissão para a operação";

        log.warn("Acesso negado: usuário={} perfil={} rota={} motivo={}",
                usuarioAtual.username(), usuarioAtual.perfil(), rota, motivo);
        try {
            auditoriaService.registrarAcessoNegado(rota, motivo);
        } catch (RuntimeException e) {
            log.error("Falha ao registrar acesso negado na auditoria", e);
        }

        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            ErroResponse erro = new ErroResponse(LocalDateTime.now(), 403, "Acesso negado",
                    motivo + ".", request.getRequestURI(), List.of());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), erro);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, motivo);
        }
    }
}
