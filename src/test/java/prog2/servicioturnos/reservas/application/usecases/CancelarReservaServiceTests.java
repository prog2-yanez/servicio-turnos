package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.exception.CancelacionNoConfirmadaException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.ReservaNoCancelableException;
import prog2.servicioturnos.reservas.domain.exception.ResultadoInciertoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;

class CancelarReservaServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final Instant ANTES = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final CuentasPacientes cuentas = mock(CuentasPacientes.class);
    private final ReservasCatedra catedra = mock(ReservasCatedra.class);
    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final CancelarReservaService servicio =
            new CancelarReservaService(cuentas, procesos, catedra, Clock.fixed(AHORA, ZoneOffset.UTC));

    private long confirmadaId;

    @BeforeEach
    void unaReservaConfirmadaDeAna() {
        when(cuentas.obtener("ana")).thenReturn(new Paciente(4, "Ana", "Gomez"));
        when(cuentas.obtener("beto")).thenReturn(new Paciente(5, "Beto", "Diaz"));
        confirmadaId = procesos.guardar(enCurso(4).confirmado("500", ANTES)).id();
    }

    @Test
    void cancelaEnLaCatedraYQuedaCanceladaConElMotivo() {
        when(catedra.cancelar("rp-9", "No puedo ir")).thenReturn("500");

        ProcesoReserva cancelada = servicio.cancelar("ana", confirmadaId, "No puedo ir");

        assertThat(cancelada.estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(cancelada.motivo()).isEqualTo("No puedo ir");
        assertThat(cancelada.reservationId()).isEqualTo("500");
        assertThat(cancelada.actualizadoEn()).isEqualTo(AHORA);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CANCELADO);
    }

    // Una reserva ajena se responde como inexistente, igual que al consultarla.
    @Test
    void unaReservaAjenaNoSeEncuentraYNoSeLlamaALaCatedra() {
        assertThatThrownBy(() -> servicio.cancelar("beto", confirmadaId, null))
                .isInstanceOf(ProcesoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.cancelar("ana", 999, null))
                .isInstanceOf(ProcesoNoEncontradoException.class);
        verifyNoInteractions(catedra);
        assertThat(procesos.buscarPorId(confirmadaId)).get()
                .extracting(ProcesoReserva::estado).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void cancelarDosVecesDevuelveLaMismaReservaSinVolverALlamarALaCatedra() {
        when(catedra.cancelar("rp-9", null)).thenReturn("500");
        servicio.cancelar("ana", confirmadaId, null);

        ProcesoReserva otraVez = servicio.cancelar("ana", confirmadaId, null);

        assertThat(otraVez.estado()).isEqualTo(EstadoProceso.CANCELADO);
        verify(catedra).cancelar("rp-9", null);
    }

    @Test
    void unProcesoSinConfirmarNoSeCancelaSeDejaVencer() {
        long abierto = procesos.guardar(enCurso(5)).id();

        assertThatThrownBy(() -> servicio.cancelar("beto", abierto, null))
                .isInstanceOf(ReservaNoCancelableException.class);
        verifyNoInteractions(catedra);
    }

    @Test
    void siLaCatedraNoLaPuedeCancelarSigueConfirmada() {
        when(catedra.cancelar("rp-9", null)).thenThrow(new ReservaNoCancelableException());

        assertThatThrownBy(() -> servicio.cancelar("ana", confirmadaId, null))
                .isInstanceOf(ReservaNoCancelableException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    // Timeout, pero la cátedra sí la canceló: se comprueba en su lista antes de informarlo.
    @Test
    void conTimeoutVerificaEnLaCatedraYSiFiguraCanceladaLaMarca() {
        when(catedra.cancelar("rp-9", null)).thenThrow(new ResultadoInciertoException("sin respuesta"));
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                reserva("501", "rp-otro", EstadoReservaCatedra.CANCELADA),
                reserva("500", "rp-9", EstadoReservaCatedra.CANCELADA)));

        ProcesoReserva cancelada = servicio.cancelar("ana", confirmadaId, null);

        assertThat(cancelada.estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(cancelada.reservationId()).isEqualTo("500");
    }

    @Test
    void conTimeoutYLaReservaTodaviaConfirmadaNoSeInformaComoCancelada() {
        when(catedra.cancelar("rp-9", null)).thenThrow(new ResultadoInciertoException("sin respuesta"));
        when(catedra.deUnPaciente("4")).thenReturn(List.of(reserva("500", "rp-9", EstadoReservaCatedra.CONFIRMADA)));

        assertThatThrownBy(() -> servicio.cancelar("ana", confirmadaId, null))
                .isInstanceOf(CancelacionNoConfirmadaException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void conTimeoutYSinPoderVerificarNoSeInformaComoCancelada() {
        when(catedra.cancelar("rp-9", null)).thenThrow(new ResultadoInciertoException("sin respuesta"));
        when(catedra.deUnPaciente("4")).thenThrow(new CatedraNoDisponibleException());

        assertThatThrownBy(() -> servicio.cancelar("ana", confirmadaId, null))
                .isInstanceOf(CancelacionNoConfirmadaException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void siLaCatedraRechazaElPedidoSigueConfirmada() {
        when(catedra.cancelar(any(), any())).thenThrow(new CatedraNoDisponibleException());

        assertThatThrownBy(() -> servicio.cancelar("ana", confirmadaId, null))
                .isInstanceOf(CatedraNoDisponibleException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    // AppointmentCancelled llegó mientras se esperaba la respuesta: lo que llegó primero queda.
    @Test
    void siElEventoLlegoPrimeroNoSeVuelveATocar() {
        when(catedra.cancelar("rp-9", "Otro motivo")).thenAnswer(invocacion -> {
            procesos.guardar(procesos.ultimo().cancelado("500", "Motivo del evento", ANTES));
            return "500";
        });

        ProcesoReserva cancelada = servicio.cancelar("ana", confirmadaId, "Otro motivo");

        assertThat(cancelada.motivo()).isEqualTo("Motivo del evento");
        assertThat(cancelada.actualizadoEn()).isEqualTo(ANTES);
    }

    private static ProcesoReserva enCurso(long usuarioId) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, ANTES)
                .conHold("77", "rp-9", VENCE, ANTES)
                .esperandoTelefono(VENCE, ANTES);
    }

    private static ReservaCatedra reserva(String reservationId, String reservationProcessId,
            EstadoReservaCatedra estado) {
        return new ReservaCatedra(reservationId, reservationProcessId, "4", LocalDate.of(2026, 11, 3),
                LocalTime.of(9, 0), LocalTime.of(9, 30), 15, "Ana", "Gomez", estado, ANTES, null, null);
    }
}
