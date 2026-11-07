package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.config.RecuperacionProperties;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ResultadoRecuperacion;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;

// Simula un reinicio: los procesos quedaron guardados a medias y la recuperación los encuentra al arrancar.
class RecuperarProcesosServiceTests {

    private static final Instant CREADO = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");
    private static final RecuperacionProperties PROPIEDADES =
            new RecuperacionProperties(Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(15));

    private final ReservasCatedra catedra = mock(ReservasCatedra.class);
    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();

    @Test
    void sinProcesosAbiertosNoConsultaNada() {
        ResultadoRecuperacion resultado = servicioA("12:05").recuperar();

        assertThat(resultado).isEqualTo(new ResultadoRecuperacion(0, 0, 0));
        verifyNoInteractions(catedra);
    }

    @Test
    void siLaCatedraLaConfirmoQuedaConfirmadaAunqueNoHayaLlegadoElEvento() {
        long id = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        when(catedra.deUnPaciente("4")).thenReturn(List.of(reserva("500", "rp-1", EstadoReservaCatedra.CONFIRMADA)));

        ResultadoRecuperacion resultado = servicioA("12:05").recuperar();

        assertThat(resultado).isEqualTo(new ResultadoRecuperacion(1, 1, 0));
        assertThat(estado(id)).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(procesos.buscarPorId(id)).get().extracting(ProcesoReserva::reservationId).isEqualTo("500");
    }

    // Timeout al confirmar el hold: la cátedra sí lo había recibido y la reserva terminó confirmada.
    @Test
    void unProcesoInciertoSeReconciliaConLosIdentificadoresGuardados() {
        long id = procesos.guardar(conHold(4, "rp-1").incierto(CREADO)).id();
        when(catedra.deUnPaciente("4")).thenReturn(List.of(reserva("500", "rp-1", EstadoReservaCatedra.CONFIRMADA)));

        servicioA("12:05").recuperar();

        assertThat(estado(id)).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void siLaCatedraLaCanceloOFalloQuedaCanceladoOInvalido() {
        long cancelado = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        long fallido = procesos.guardar(telefonoEnviado(5, "rp-2")).id();
        when(catedra.deUnPaciente("4")).thenReturn(List.of(reserva("500", "rp-1", EstadoReservaCatedra.CANCELADA)));
        when(catedra.deUnPaciente("5")).thenReturn(List.of(reserva("501", "rp-2", EstadoReservaCatedra.FALLIDA)));

        servicioA("12:05").recuperar();

        assertThat(estado(cancelado)).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(estado(fallido)).isEqualTo(EstadoProceso.INVALIDO);
        assertThat(procesos.buscarPorId(fallido)).get().extracting(ProcesoReserva::motivo)
                .isEqualTo(RecuperarProcesosService.MOTIVO_FALLIDA);
    }

    // Todavía esperando el teléfono y sin vencer: lo resuelve el evento de Kafka, no la recuperación.
    @Test
    void unProcesoEnCursoQueNoVencioSeDejaComoEsta() {
        long id = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        when(catedra.deUnPaciente("4"))
                .thenReturn(List.of(reserva("500", "rp-1", EstadoReservaCatedra.TELEFONO_PENDIENTE)));

        ResultadoRecuperacion resultado = servicioA("12:11").recuperar();

        assertThat(resultado).isEqualTo(new ResultadoRecuperacion(1, 0, 1));
        assertThat(estado(id)).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
    }

    // venceEn más el margen ya pasó y la cátedra no informó un resultado final.
    @Test
    void unProcesoQuePasoSuVencimientoSinResultadoSeCierraComoVencido() {
        long id = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        when(catedra.deUnPaciente("4"))
                .thenReturn(List.of(reserva("500", "rp-1", EstadoReservaCatedra.TELEFONO_PENDIENTE)));

        servicioA("12:12").recuperar();

        assertThat(estado(id)).isEqualTo(EstadoProceso.VENCIDO);
    }

    // Si en realidad estaba confirmada, AppointmentConfirmed lo corrige después (VENCIDO -> CONFIRMADO).
    @Test
    void conLaCatedraCaidaLosVencidosSeCierranIgualYLosDemasSeReintentan() {
        long vencido = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        long enCurso = procesos.guardar(telefonoEnviado(5, "rp-2").telefonoRechazado(
                Instant.parse("2026-11-02T12:30:00Z"), CREADO)).id();
        when(catedra.deUnPaciente(any())).thenThrow(new CatedraNoDisponibleException());

        ResultadoRecuperacion resultado = servicioA("12:15").recuperar();

        assertThat(resultado).isEqualTo(new ResultadoRecuperacion(2, 1, 1));
        assertThat(estado(vencido)).isEqualTo(EstadoProceso.VENCIDO);
        assertThat(estado(enCurso)).isEqualTo(EstadoProceso.TELEFONO_RECHAZADO);
    }

    // Timeout al crear el hold o reinicio antes de tenerlo: no hay identificadores para consultar a la cátedra.
    @Test
    void sinHoldSeCierraComoVencidoRecienDespuesDeLaEsperaSinConsultarALaCatedra() {
        long incierto = procesos.guardar(nuevo(4).incierto(CREADO)).id();
        long iniciado = procesos.guardar(nuevo(5)).id();

        servicioA("12:14").recuperar();
        assertThat(estado(incierto)).isEqualTo(EstadoProceso.INCIERTO);
        assertThat(estado(iniciado)).isEqualTo(EstadoProceso.INICIADO);

        servicioA("12:15").recuperar();
        assertThat(estado(incierto)).isEqualTo(EstadoProceso.VENCIDO);
        assertThat(estado(iniciado)).isEqualTo(EstadoProceso.VENCIDO);
        verifyNoInteractions(catedra);
    }

    @Test
    void nuncaReabreUnProcesoConEstadoFinal() {
        long confirmado = procesos.guardar(telefonoEnviado(4, "rp-1").confirmado("500", CREADO)).id();
        long rechazado = procesos.guardar(nuevo(5).rechazado(CREADO)).id();

        ResultadoRecuperacion resultado = servicioA("13:00").recuperar();

        assertThat(resultado.revisados()).isZero();
        assertThat(estado(confirmado)).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(estado(rechazado)).isEqualTo(EstadoProceso.RECHAZADO);
        verify(catedra, never()).deUnPaciente(any());
    }

    // Un evento llegó mientras se consultaba a la cátedra: manda el evento y la recuperación no lo pisa.
    @Test
    void siUnEventoCambioElProcesoMientrasSeRevisabaNoLoPisa() {
        long id = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        when(catedra.deUnPaciente("4")).thenAnswer(invocacion -> {
            procesos.guardar(procesos.buscarPorId(id).orElseThrow()
                    .cancelado("500", "Motivo del evento", Instant.parse("2026-11-02T12:14:00Z")));
            return List.of();
        });

        ResultadoRecuperacion resultado = servicioA("12:15").recuperar();

        assertThat(estado(id)).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(resultado.resueltos()).isEqualTo(1);
    }

    @Test
    void consultaUnaSolaVezPorUsuarioYSoloSusReservas() {
        long primero = procesos.guardar(telefonoEnviado(4, "rp-1")).id();
        procesos.guardar(procesos.buscarPorId(primero).orElseThrow().confirmado("499", CREADO));
        long segundo = procesos.guardar(telefonoEnviado(4, "rp-2")).id();
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                reserva("499", "rp-1", EstadoReservaCatedra.CONFIRMADA),
                reserva("500", "rp-2", EstadoReservaCatedra.CONFIRMADA)));

        servicioA("12:05").recuperar();

        assertThat(estado(segundo)).isEqualTo(EstadoProceso.CONFIRMADO);
        verify(catedra).deUnPaciente("4");
    }

    private RecuperarProcesosService servicioA(String hora) {
        Instant ahora = Instant.parse("2026-11-02T" + hora + ":00Z");
        return new RecuperarProcesosService(procesos, catedra, PROPIEDADES, Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private EstadoProceso estado(long id) {
        return procesos.buscarPorId(id).orElseThrow().estado();
    }

    private static ProcesoReserva nuevo(long usuarioId) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, CREADO);
    }

    private static ProcesoReserva conHold(long usuarioId, String reservationProcessId) {
        return nuevo(usuarioId).conHold("h-" + reservationProcessId, reservationProcessId, VENCE, CREADO);
    }

    private static ProcesoReserva telefonoEnviado(long usuarioId, String reservationProcessId) {
        return conHold(usuarioId, reservationProcessId).esperandoTelefono(VENCE, CREADO)
                .telefonoPedido("ev-pedido", VENCE, CREADO)
                .telefonoEnviado(CREADO);
    }

    private static ReservaCatedra reserva(String reservationId, String reservationProcessId,
            EstadoReservaCatedra estado) {
        return new ReservaCatedra(reservationId, reservationProcessId, "4", LocalDate.of(2026, 11, 3),
                LocalTime.of(9, 0), LocalTime.of(9, 30), 15, "Ana", "Gomez", estado, CREADO, null, null);
    }
}
