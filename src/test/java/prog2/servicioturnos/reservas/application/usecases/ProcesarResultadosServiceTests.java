package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EventoCatedra;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.EventosProcesados;

// CU-06: eventos de resultado, repetidos y fuera de orden.
class ProcesarResultadosServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final Set<String> registrados = new HashSet<>();
    private final EventosProcesados eventosProcesados = (eventId, tipo) -> registrados.add(eventId);
    private final ProcesarAccionesCatedraService servicio =
            new ProcesarAccionesCatedraService(procesos, eventosProcesados, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void unTelefonoEnviado() {
        procesos.guardar(ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, AHORA)
                .conHold("77", "rp-9", VENCE, AHORA)
                .esperandoTelefono(VENCE, AHORA)
                .telefonoPedido("ev-pedido", VENCE, AHORA)
                .telefonoEnviado(AHORA));
    }

    @Test
    void cadaResultadoLlevaAlProcesoASuEstadoFinal() {
        servicio.confirmado(evento("ev-1", "AppointmentConfirmed", "res-500", null));
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(procesos.ultimo().reservationId()).isEqualTo("res-500");

        servicio.cancelado(evento("ev-2", "AppointmentCancelled", "res-500", "Pedido del paciente"));
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(procesos.ultimo().motivo()).isEqualTo("Pedido del paciente");
    }

    @Test
    void elVencimientoYLaInvalidacionTerminanUnProcesoAbierto() {
        servicio.vencido(evento("ev-1", "AppointmentProcessExpired", null, "HOLD_EXPIRED"));
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.VENCIDO);

        RepositorioProcesosEnMemoria otros = new RepositorioProcesosEnMemoria();
        otros.guardar(procesos.historial().getFirst());
        new ProcesarAccionesCatedraService(otros, (id, tipo) -> true, Clock.fixed(AHORA, ZoneOffset.UTC))
                .invalido(evento("ev-2", "AppointmentProcessInvalid", null, "SLOT_NOT_AVAILABLE"));
        assertThat(otros.ultimo().estado()).isEqualTo(EstadoProceso.INVALIDO);
        assertThat(otros.ultimo().motivo()).isEqualTo("SLOT_NOT_AVAILABLE");
    }

    @Test
    void elMismoEventoDosVecesSeAplicaUnaSolaVez() {
        EventoCatedra confirmado = evento("ev-1", "AppointmentConfirmed", "res-500", null);

        servicio.confirmado(confirmado);
        servicio.confirmado(confirmado);

        assertThat(procesos.historial()).hasSize(2);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void unVencimientoQueLlegaDespuesDeLaConfirmacionNoLaDeshace() {
        servicio.confirmado(evento("ev-1", "AppointmentConfirmed", "res-500", null));
        servicio.vencido(evento("ev-2", "AppointmentProcessExpired", null, "HOLD_EXPIRED"));
        servicio.telefonoPedido(new EventoCatedra("ev-3", "AdditionalInformationRequested", "rp-9", VENCE, null,
                null));

        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(registrados).containsExactlyInAnyOrder("ev-1", "ev-2", "ev-3");
    }

    @Test
    void unaCancelacionQueLlegaAntesQueLaConfirmacionGana() {
        servicio.cancelado(evento("ev-2", "AppointmentCancelled", "res-500", null));
        servicio.confirmado(evento("ev-1", "AppointmentConfirmed", "res-500", null));

        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(procesos.ultimo().reservationId()).isEqualTo("res-500");
    }

    @Test
    void laConfirmacionGanaSobreUnVencimientoQueTurnosDecidioPorSuCuenta() {
        procesos.guardar(procesos.ultimo().vencido(AHORA));

        servicio.confirmado(evento("ev-1", "AppointmentConfirmed", "res-500", null));

        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void unMotivoMuyLargoSeRecorta() {
        servicio.invalido(evento("ev-1", "AppointmentProcessInvalid", null, "x".repeat(800)));

        assertThat(procesos.ultimo().motivo()).hasSize(ProcesarAccionesCatedraService.LARGO_MAXIMO_MOTIVO);
    }

    private static EventoCatedra evento(String eventId, String tipo, String reservationId, String motivo) {
        return new EventoCatedra(eventId, tipo, "rp-9", null, reservationId, motivo);
    }
}
