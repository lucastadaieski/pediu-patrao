package com.umc.pediupatrao.exception;

import com.umc.pediupatrao.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e, HttpServletRequest request) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados.", request, detalhes);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> validacaoParametros(HandlerMethodValidationException e,
                                                            HttpServletRequest request) {
        List<String> detalhes = e.getAllErrors().stream()
                .map(err -> err.getDefaultMessage())
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", "Verifique os campos informados.", request, detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "Corpo da requisição ausente ou em formato inválido (verifique o JSON e os valores dos enums).",
                request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> argumentoInvalido(IllegalArgumentException e, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage(), request, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String erro, String mensagem,
                                                  HttpServletRequest request, List<String> detalhes) {
        ErroResponse corpo = new ErroResponse(LocalDateTime.now(), status.value(), erro, mensagem,
                request.getRequestURI(), detalhes);
        return ResponseEntity.status(status).body(corpo);
    }
}
