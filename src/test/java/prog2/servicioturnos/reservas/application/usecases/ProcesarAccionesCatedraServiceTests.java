package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EventoCatedra;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.EventosProcesados;

class ProcesarAccionesCatedraServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final Set<String> registrados = new HashSet<>();
    private final EventosProcesados eventosProcesados = (eventId, tipo) -> registrados.add(eventId);
    private final ProcesarAccionesCatedraService servicio =
            new ProcesarAccionesCatedraService(procesos, eventosProcesados, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void elPedidoDeTelefonoDejaElProcesoEsperandoConElEventId() {
        procesos.guardar(esperandoTelefono());

        servicio.telefonoPedido(pedido("ev-1", "rp-9"));

        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(procesos.ultimo().requestEventId()).isEqualTo("ev-1");
    }

    @Test
    void unEventoDeUnProcesoDesconocidoSeIgnora() {
        procesos.guardar(esperandoTelefono());

        servicio.telefonoPedido(pedido("ev-1", "otro"));

        assertThat(procesos.historial()).hasSize(1);
    }

    @Test
    void unEventoTardioNoReabreUnProcesoTerminado() {
        procesos.guardar(esperandoTelefono().vencido(AHORA));

        servicio.telefonoPedido(pedido("ev-1", "rp-9"));

        assertThat(procesos.historial()).hasSize(1);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.VENCIDO);
    }

    @Test
    void elRechazoDelTelefonoPermiteCorregirlo() {
        procesos.guardar(esperandoTelefono().telefonoPedido("ev-1", VENCE, AHORA).telefonoEnviado(AHORA));

        servicio.telefonoRechazado(rechazo("ev-2", VENCE.plusSeconds(300)));

        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.TELEFONO_RECHAZADO);
        assertThat(procesos.ultimo().venceEn()).isEqualTo(VENCE.plusSeconds(300));
    }

    @Test
    void unRechazoSinTelefonoEnviadoSeIgnora() {
        procesos.guardar(esperandoTelefono());

        servicio.telefonoRechazado(rechazo("ev-2", VENCE));

        assertThat(procesos.historial()).hasSize(1);
    }

    private static EventoCatedra pedido(String eventId, String reservationProcessId) {
        return new EventoCatedra(eventId, "AdditionalInformationRequested", reservationProcessId, VENCE, null, null);
    }

    private static EventoCatedra rechazo(String eventId, Instant venceEn) {
        return new EventoCatedra(eventId, "AdditionalInformationRejected", "rp-9", venceEn, null,
                "INVALID_PHONE_NUMBER");
    }

    private static ProcesoReserva esperandoTelefono() {
        return ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, AHORA)
                .conHold("77", "rp-9", VENCE, AHORA)
                .esperandoTelefono(VENCE, AHORA);
    }
}
