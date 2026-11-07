package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.disponibilidad.domain.exception.ProfesionalDeshabilitadoException;
import prog2.servicioturnos.disponibilidad.domain.model.TurnoLibre;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;
import prog2.servicioturnos.disponibilidad.domain.ports.in.ConsultarTurnosLibresUseCase;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.exception.DatosDeCuentaIncompletosException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoAbiertoException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoVencidoException;
import prog2.servicioturnos.reservas.domain.exception.ReservaRechazadaException;
import prog2.servicioturnos.reservas.domain.exception.ResultadoInciertoException;
import prog2.servicioturnos.reservas.domain.exception.TurnoNoDisponibleException;
import prog2.servicioturnos.reservas.domain.exception.TurnoOcupadoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.HoldCreado;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.PedidoReserva;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.HoldsCatedra;

class IniciarReservaServiceTests {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 3);
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final PedidoReserva PEDIDO = new PedidoReserva(15, MARTES, NUEVE);
    private static final Instant VENCE_HOLD = Instant.parse("2026-11-02T12:10:00Z");
    private static final Instant VENCE_CONFIRMADO = Instant.parse("2026-11-02T12:15:00Z");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);

    private final CuentasPacientes cuentas = mock(CuentasPacientes.class);
    private final ConsultarTurnosLibresUseCase turnosLibres = mock(ConsultarTurnosLibresUseCase.class);
    private final HoldsCatedra holds = mock(HoldsCatedra.class);
    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final IniciarReservaService servicio =
            new IniciarReservaService(cuentas, procesos, turnosLibres, holds, RELOJ);

    @BeforeEach
    void preparar() {
        when(cuentas.obtener("ana")).thenReturn(new Paciente(4, "Ana", "Gomez"));
        when(turnosLibres.consultar(15, MARTES)).thenReturn(new TurnosLibresDelProfesional(15, MARTES, 7,
                List.of(new TurnoLibre(NUEVE, LocalTime.of(9, 30)))));
    }

    @Test
    void conTodoBienQuedaEsperandoElTelefonoYGuardaCadaPaso() {
        when(holds.crearHold(15, MARTES, NUEVE, "4")).thenReturn(new HoldCreado("h-77", "rp-9", VENCE_HOLD));
        when(holds.confirmar("h-77", "4", "Ana", "Gomez")).thenReturn(VENCE_CONFIRMADO);

        ProcesoReserva proceso = servicio.iniciar("ana", PEDIDO);

        assertThat(proceso.estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.usuarioId()).isEqualTo(4);
        assertThat(proceso.versionCatalogo()).isEqualTo(7);
        assertThat(proceso.holdId()).isEqualTo("h-77");
        assertThat(proceso.reservationProcessId()).isEqualTo("rp-9");
        assertThat(proceso.venceEn()).isEqualTo(VENCE_CONFIRMADO);
        // Se guardó INICIADO antes de la cátedra, después con el hold y por último esperando el teléfono.
        assertThat(procesos.historial()).extracting(ProcesoReserva::estado).containsExactly(
                EstadoProceso.INICIADO, EstadoProceso.INICIADO, EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(procesos.historial().get(0).holdId()).isNull();
    }

    @Test
    void sinNombreOApellidoNoRegistraNiLlamaALaCatedra() {
        when(cuentas.obtener("ana")).thenReturn(new Paciente(4, "Ana", null));

        assertThatThrownBy(() -> servicio.iniciar("ana", PEDIDO)).isInstanceOf(DatosDeCuentaIncompletosException.class);
        assertThat(procesos.historial()).isEmpty();
        verifyNoInteractions(holds, turnosLibres);
    }

    @Test
    void conUnProcesoAbiertoResponde409SinTocarNada() {
        procesos.conProcesoAbierto();

        assertThatThrownBy(() -> servicio.iniciar("ana", PEDIDO)).isInstanceOf(ProcesoAbiertoException.class);
        assertThat(procesos.historial()).isEmpty();
        verifyNoInteractions(holds, turnosLibres);
    }

    @Test
    void siElTurnoYaNoEstaLibreNoRegistraNada() {
        PedidoReserva otraHora = new PedidoReserva(15, MARTES, LocalTime.of(10, 0));

        assertThatThrownBy(() -> servicio.iniciar("ana", otraHora)).isInstanceOf(TurnoNoDisponibleException.class);
        assertThat(procesos.historial()).isEmpty();
        verifyNoInteractions(holds);
    }

    @Test
    void siElCatalogoDiceQueElProfesionalEstaDeshabilitadoNoRegistraNada() {
        when(turnosLibres.consultar(15, MARTES)).thenThrow(new ProfesionalDeshabilitadoException(15));

        assertThatThrownBy(() -> servicio.iniciar("ana", PEDIDO))
                .isInstanceOf(ProfesionalDeshabilitadoException.class);
        assertThat(procesos.historial()).isEmpty();
    }

    static List<RuntimeException> rechazosDelHold() {
        return List.of(new TurnoOcupadoException(), new TurnoNoDisponibleException(),
                new ReservaRechazadaException(), new CatedraNoDisponibleException());
    }

    @ParameterizedTest
    @MethodSource("rechazosDelHold")
    void unRechazoDelHoldDejaElProcesoRechazadoYPropagaElError(RuntimeException rechazo) {
        when(holds.crearHold(anyLong(), any(), any(), anyString())).thenThrow(rechazo);

        assertThatThrownBy(() -> servicio.iniciar("ana", PEDIDO)).isSameAs(rechazo);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.RECHAZADO);
        verify(holds, never()).confirmar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void siElHoldVencioAntesDeConfirmarQuedaVencido() {
        when(holds.crearHold(15, MARTES, NUEVE, "4")).thenReturn(new HoldCreado("h-77", "rp-9", VENCE_HOLD));
        when(holds.confirmar("h-77", "4", "Ana", "Gomez")).thenThrow(new ProcesoVencidoException());

        assertThatThrownBy(() -> servicio.iniciar("ana", PEDIDO)).isInstanceOf(ProcesoVencidoException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.VENCIDO);
        assertThat(procesos.ultimo().holdId()).isEqualTo("h-77");
    }

    @Test
    void siLaCatedraNoContestaElHoldQuedaInciertoSinReintentar() {
        when(holds.crearHold(15, MARTES, NUEVE, "4")).thenThrow(new ResultadoInciertoException("timeout"));

        ProcesoReserva proceso = servicio.iniciar("ana", PEDIDO);

        assertThat(proceso.estado()).isEqualTo(EstadoProceso.INCIERTO);
        verify(holds, never()).confirmar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void siLaCatedraNoContestaLaConfirmacionQuedaInciertoConElHoldGuardado() {
        when(holds.crearHold(15, MARTES, NUEVE, "4")).thenReturn(new HoldCreado("h-77", "rp-9", VENCE_HOLD));
        when(holds.confirmar("h-77", "4", "Ana", "Gomez")).thenThrow(new ResultadoInciertoException("HTTP 503"));

        ProcesoReserva proceso = servicio.iniciar("ana", PEDIDO);

        assertThat(proceso.estado()).isEqualTo(EstadoProceso.INCIERTO);
        assertThat(proceso.holdId()).isEqualTo("h-77");
        assertThat(proceso.reservationProcessId()).isEqualTo("rp-9");
    }
}
