package prog2.servicioturnos.disponibilidad.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.config.TurnosProperties;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.exception.FechaPasadaException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalNoEncontradoException;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalConTurnosLibres;
import prog2.servicioturnos.disponibilidad.domain.model.ProfesionalResumen;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.disponibilidad.domain.ports.out.CatalogoClient;

class BuscarProfesionalesConTurnosLibresServiceTests {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);
    private static final Clock LUNES = Clock.fixed(Instant.parse("2026-11-02T11:00:00Z"), ZoneOffset.UTC);
    private static final TurnosProperties PROPIEDADES =
            new TurnosProperties(ZoneId.of("America/Argentina/Buenos_Aires"));

    private final CatalogoClient catalogo = mock(CatalogoClient.class);
    private final ConsultarTurnosLibresUseCase consultarTurnosLibres = mock(ConsultarTurnosLibresUseCase.class);
    private final BuscarProfesionalesConTurnosLibresService servicio =
            new BuscarProfesionalesConTurnosLibresService(catalogo, consultarTurnosLibres, LUNES, PROPIEDADES);

    @Test
    void devuelveSoloLosProfesionalesConAlMenosUnTurnoLibre() {
        when(catalogo.listarHabilitados(1)).thenReturn(List.of(
                new ProfesionalResumen(15, 1, "Ana", "Gomez"),
                new ProfesionalResumen(16, 1, "Luis", "Paz")));
        when(consultarTurnosLibres.consultar(15, MARTES)).thenReturn(libres(15, 2));
        when(consultarTurnosLibres.consultar(16, MARTES)).thenReturn(libres(16, 0));

        assertThat(servicio.buscar(1, MARTES))
                .containsExactly(new ProfesionalConTurnosLibres(15, 1, "Ana", "Gomez", 2));
    }

    @Test
    void unaCategoriaSinProfesionalesDevuelveUnaListaVacia() {
        when(catalogo.listarHabilitados(1)).thenReturn(List.of());

        assertThat(servicio.buscar(1, MARTES)).isEmpty();
    }

    @Test
    void saltaAlProfesionalQueSeDeshabilitoODesaparecioEnElMedio() {
        when(catalogo.listarHabilitados(1)).thenReturn(List.of(
                new ProfesionalResumen(15, 1, "Ana", "Gomez"),
                new ProfesionalResumen(16, 1, "Luis", "Paz"),
                new ProfesionalResumen(17, 1, "Eva", "Ruiz")));
        when(consultarTurnosLibres.consultar(15, MARTES)).thenThrow(new ProfesionalDeshabilitadoException(15));
        when(consultarTurnosLibres.consultar(16, MARTES)).thenThrow(new ProfesionalNoEncontradoException(16));
        when(consultarTurnosLibres.consultar(17, MARTES)).thenReturn(libres(17, 1));

        assertThat(servicio.buscar(1, MARTES)).extracting(ProfesionalConTurnosLibres::id).containsExactly(17L);
    }

    @Test
    void siLaCatedraNoRespondeFallaTodaLaBusqueda() {
        when(catalogo.listarHabilitados(1)).thenReturn(List.of(new ProfesionalResumen(15, 1, "Ana", "Gomez")));
        when(consultarTurnosLibres.consultar(15, MARTES)).thenThrow(new CatedraNoDisponibleException());

        assertThatThrownBy(() -> servicio.buscar(1, MARTES)).isInstanceOf(CatedraNoDisponibleException.class);
    }

    @Test
    void unaFechaPasadaSeRechazaSinConsultarAlCatalogo() {
        assertThatThrownBy(() -> servicio.buscar(1, LocalDate.of(2026, 11, 1)))
                .isInstanceOf(FechaPasadaException.class);
        verifyNoInteractions(catalogo, consultarTurnosLibres);
    }

    private static TurnosLibresDelProfesional libres(long profesionalId, int cantidad) {
        List<TurnoLibre> turnos = IntStream.range(0, cantidad)
                .mapToObj(i -> new TurnoLibre(LocalTime.of(9 + i, 0), LocalTime.of(9 + i, 30)))
                .toList();
        return new TurnosLibresDelProfesional(profesionalId, MARTES, 7, turnos);
    }
}
