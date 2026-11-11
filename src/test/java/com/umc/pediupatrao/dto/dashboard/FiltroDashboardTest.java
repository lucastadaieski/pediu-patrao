package com.umc.pediupatrao.dto.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.enums.StatusPedido;
import com.umc.pediupatrao.entity.enums.TipoAtendimento;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Regras do filtro de período do dashboard (RF05 e RF07). */
class FiltroDashboardTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Test
    void semDatasUsaUltimos30DiasIncluindoHoje() {
        FiltroDashboard f = FiltroDashboard.de(null, null, null, null, HOJE);
        assertEquals(LocalDate.of(2026, 10, 12), f.inicio());
        assertEquals(HOJE, f.fim());
        assertEquals(30, f.dias());
    }

    @Test
    void soComFimUsa30DiasAteOFim() {
        FiltroDashboard f = FiltroDashboard.de(null, LocalDate.of(2026, 10, 31), null, null, HOJE);
        assertEquals(LocalDate.of(2026, 10, 2), f.inicio());
    }

    @Test
    void soComInicioVaiAteHoje() {
        FiltroDashboard f = FiltroDashboard.de(LocalDate.of(2026, 11, 1), null, null, null, HOJE);
        assertEquals(HOJE, f.fim());
        assertEquals(10, f.dias());
    }

    @Test
    void inicioDepoisDoFimEhRecusado() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> FiltroDashboard.de(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), null, null, HOJE));
        assertTrue(e.getMessage().contains("posterior"));
    }

    @Test
    void periodoMaiorQueUmAnoEhRecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> FiltroDashboard.de(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 5), null, null, HOJE));
    }

    @Test
    void periodoAnteriorTemMesmoTamanhoEMantemOsFiltros() {
        FiltroDashboard f = new FiltroDashboard(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10),
                StatusPedido.FINALIZADO, TipoAtendimento.ENTREGA);
        FiltroDashboard anterior = f.periodoAnterior();
        assertEquals(LocalDate.of(2026, 10, 22), anterior.inicio());
        assertEquals(LocalDate.of(2026, 10, 31), anterior.fim());
        assertEquals(f.dias(), anterior.dias());
        assertEquals(StatusPedido.FINALIZADO, anterior.status());
        assertEquals(TipoAtendimento.ENTREGA, anterior.tipoAtendimento());
    }

    @Test
    void fimEhInclusivoAteOUltimoMinutoDoDia() {
        FiltroDashboard f = new FiltroDashboard(HOJE, HOJE, null, null);
        assertTrue(f.contem(HOJE.atTime(0, 0)));
        assertTrue(f.contem(HOJE.atTime(23, 59, 59)));
        assertFalse(f.contem(HOJE.plusDays(1).atStartOfDay()));
        assertFalse(f.contem(LocalDateTime.of(2026, 11, 9, 23, 59)));
    }
}
