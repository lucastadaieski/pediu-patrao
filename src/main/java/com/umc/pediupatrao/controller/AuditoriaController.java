package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.enums.TipoOperacao;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<RegistroAuditoria> consultar(@RequestParam(required = false) String entidade,
                                             @RequestParam(required = false) String entidadeId,
                                             @RequestParam(required = false) String usuario,
                                             @RequestParam(required = false) TipoOperacao operacao) {
        return auditoriaService.consultar(entidade, entidadeId, usuario, operacao);
    }
}
