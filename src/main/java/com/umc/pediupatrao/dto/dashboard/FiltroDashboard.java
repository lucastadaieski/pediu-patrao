package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


public record FiltroDashboard(LocalDate inicio, LocalDate fim, StatusPedido status,
                              TipoAtendimento tipoAtendimento) {

    /** Período padrão quando a tela não informa datas: últimos 30 dias, incluindo hoje. */
    public static final int DIAS_PADRAO = 30;

    /** Limite para evitar consultas pesadas demais (um ano, contando ano bissexto). */
    public static final int DIAS_MAXIMO = 366;

    public FiltroDashboard {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe a data inicial e a data final do período.");
        }
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        if (dias > DIAS_MAXIMO) {
            throw new IllegalArgumentException("O período máximo de consulta é de " + DIAS_MAXIMO + " dias.");
        }
    }

    /**
     * Monta o filtro a partir dos parâmetros da requisição, aplicando os padrões:
     * sem "fim" usa hoje; sem "inicio" usa os 30 dias que terminam em "fim".
     */
    public static FiltroDashboard de(LocalDate inicio, LocalDate fim, StatusPedido status,
                                     TipoAtendimento tipoAtendimento, LocalDate hoje) {
        LocalDate fimEfetivo = fim != null ? fim : (inicio != null && inicio.isAfter(hoje) ? inicio : hoje);
        LocalDate inicioEfetivo = inicio != null ? inicio : fimEfetivo.minusDays(DIAS_PADRAO - 1L);
        return new FiltroDashboard(inicioEfetivo, fimEfetivo, status, tipoAtendimento);
    }

    /** Quantidade de dias do período (inclui as duas pontas). */
    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    public LocalDateTime inicioDataHora() {
        return inicio.atStartOfDay();
    }

    /** Limite superior EXCLUSIVO: meia-noite do dia seguinte ao "fim". */
    public LocalDateTime fimExclusivo() {
        return fim.plusDays(1).atStartOfDay();
    }

    /** Período de mesmo tamanho imediatamente anterior, usado na comparação do RF01. */
    public FiltroDashboard periodoAnterior() {
        LocalDate fimAnterior = inicio.minusDays(1);
        return new FiltroDashboard(fimAnterior.minusDays(dias() - 1), fimAnterior, status, tipoAtendimento);
    }

    public boolean contem(LocalDateTime dataHora) {
        return dataHora != null && !dataHora.isBefore(inicioDataHora()) && dataHora.isBefore(fimExclusivo());
    }
}
