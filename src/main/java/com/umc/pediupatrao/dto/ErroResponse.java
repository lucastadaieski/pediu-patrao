package com.umc.pediupatrao.dto;

import java.time.LocalDateTime;
import java.util.List;


public record ErroResponse(LocalDateTime timestamp, int status, String erro, String mensagem,
                           String caminho, List<String> detalhes) {
}
