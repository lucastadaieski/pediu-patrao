package com.umc.pediupatrao.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

/** Opções válidas para montar os campos de filtro da tela (RF05). */
public record OpcoesFiltro(LocalDate inicioPadrao, LocalDate fimPadrao, int diasMaximo,
                           List<Opcao> status, List<Opcao> tiposAtendimento, List<Opcao> agrupamentos,
                           List<Opcao> criteriosRanking, List<String> categorias) {

    public record Opcao(String valor, String descricao) {
    }
}
