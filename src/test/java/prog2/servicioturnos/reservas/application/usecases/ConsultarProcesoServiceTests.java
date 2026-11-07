package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.SinProcesoEnCursoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;

class ConsultarProcesoServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");

    private final CuentasPacientes cuentas = mock(CuentasPacientes.class);
    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final ConsultarProcesoService servicio = new ConsultarProcesoService(cuentas, procesos);

    @BeforeEach
    void dosUsuarios() {
        when(cuentas.obtener("ana")).thenReturn(new Paciente(4, "Ana", "Gomez"));
        when(cuentas.obtener("luis")).thenReturn(new Paciente(5, "Luis", "Paz"));
    }

    @Test
    void elUsuarioVeSuPropioProceso() {
        long id = procesos.guardar(proceso(4, "09:00")).id();

        assertThat(servicio.consultar("ana", id).id()).isEqualTo(id);
    }

    @Test
    void unProcesoAjenoSeInformaComoInexistente() {
        long deAna = procesos.guardar(proceso(4, "09:00")).id();

        assertThatThrownBy(() -> servicio.consultar("luis", deAna))
                .isInstanceOf(ProcesoNoEncontradoException.class)
                .hasMessage("No tenés una reserva con id " + deAna);
        assertThatThrownBy(() -> servicio.consultar("ana", 999)).isInstanceOf(ProcesoNoEncontradoException.class);
    }

    @Test
    void enCursoDevuelveElProcesoAbiertoDelUsuarioYNoElDeOtro() {
        procesos.guardar(proceso(4, "09:00").rechazado(AHORA));
        long abiertoDeAna = procesos.guardar(proceso(4, "09:30")).id();
        procesos.guardar(proceso(5, "10:00"));

        ProcesoReserva enCurso = servicio.enCurso("ana");

        assertThat(enCurso.id()).isEqualTo(abiertoDeAna);
        assertThat(enCurso.estado()).isEqualTo(EstadoProceso.INICIADO);
    }

    @Test
    void sinProcesoAbiertoNoHayNadaEnCurso() {
        procesos.guardar(proceso(4, "09:00").rechazado(AHORA));

        assertThatThrownBy(() -> servicio.enCurso("ana")).isInstanceOf(SinProcesoEnCursoException.class);
    }

    @Test
    void unProcesoQueTerminoDespuesDeGuardarseYaNoEstaEnCurso() {
        ProcesoReserva abierto = procesos.guardar(proceso(4, "09:00"));
        procesos.guardar(abierto.vencido(AHORA));

        assertThatThrownBy(() -> servicio.enCurso("ana")).isInstanceOf(SinProcesoEnCursoException.class);
    }

    private static ProcesoReserva proceso(long usuarioId, String hora) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.parse(hora), 7, AHORA);
    }
}
