package prog2.servicioturnos.disponibilidad.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.config.TurnosProperties;
import prog2.servicioturnos.disponibilidad.domain.exception.FechaPasadaException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.model.EstadoOcupacion;
import prog2.servicioturnos.disponibilidad.domain.model.HorarioSemanal;
import prog2.servicioturnos.disponibilidad.domain.model.Ocupacion;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConAgenda;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.out.CatalogoClient;
import prog2.servicioturnos.disponibilidad.domain.ports.out.OcupacionesCatedra;

class ConsultarTurnosLibresServiceTests {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);
    private static final Clock LUNES = Clock.fixed(Instant.parse("2026-11-02T11:00:00Z"), ZoneOffset.UTC);
    private static final TurnosProperties PROPIEDADES =
            new TurnosProperties(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final CatalogoClient catalogo = mock(CatalogoClient.class);
    private final OcupacionesCatedra ocupaciones = mock(OcupacionesCatedra.class);
    private final ConsultarTurnosLibresService servicio =
            new ConsultarTurnosLibresService(catalogo, ocupaciones, LUNES, PROPIEDADES);

    @Test
    void combinaLaAgendaDelCatalogoConLasOcupacionesDeLaCatedra() {
        when(catalogo.obtenerProfesional(15)).thenReturn(profesionalQueAtiende(DayOfWeek.TUESDAY));
        when(ocupaciones.obtener(15, MARTES)).thenReturn(List.of(
                new Ocupacion(MARTES, LocalTime.of(9, 0), EstadoOcupacion.CONFIRMADA, null)));

        TurnosLibresDelProfesional libres = servicio.consultar(15, MARTES);

        assertThat(libres.profesionalId()).isEqualTo(15);
        assertThat(libres.fecha()).isEqualTo(MARTES);
        assertThat(libres.versionCatalogo()).isEqualTo(7);
        assertThat(libres.turnos()).containsExactly(new TurnoLibre(LocalTime.of(9, 30), LocalTime.of(10, 0)));
    }

    @Test
    void siNoAtiendeEseDiaNoLePreguntaALaCatedra() {
        when(catalogo.obtenerProfesional(15)).thenReturn(profesionalQueAtiende(DayOfWeek.MONDAY));

        assertThat(servicio.consultar(15, MARTES).turnos()).isEmpty();
        verifyNoInteractions(ocupaciones);
    }

    @Test
    void unaFechaPasadaSeRechazaSinConsultarANadie() {
        assertThatThrownBy(() -> servicio.consultar(15, LocalDate.of(2026, 11, 1)))
                .isInstanceOf(FechaPasadaException.class)
                .hasMessageContaining("2026-11-01");
        verifyNoInteractions(catalogo, ocupaciones);
    }

    @Test
    void unProfesionalDeshabilitadoPropagaElErrorDelCatalogo() {
        when(catalogo.obtenerProfesional(15)).thenThrow(new ProfesionalDeshabilitadoException(15));

        assertThatThrownBy(() -> servicio.consultar(15, MARTES)).isInstanceOf(ProfesionalDeshabilitadoException.class);
        verifyNoInteractions(ocupaciones);
    }

    private static ProfesionalConAgenda profesionalQueAtiende(DayOfWeek dia) {
        return new ProfesionalConAgenda(15, 1, "Ana", "Gomez",
                List.of(new HorarioSemanal(dia, LocalTime.of(9, 0), LocalTime.of(10, 0), 30)), 7);
    }
}
