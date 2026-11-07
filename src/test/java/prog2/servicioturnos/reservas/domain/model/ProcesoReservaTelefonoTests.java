package prog2.servicioturnos.reservas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ProcesoReservaTelefonoTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");
    private static final Instant NUEVO_VENCE = Instant.parse("2026-11-02T12:20:00Z");

    private final ProcesoReserva conHold = ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3),
            LocalTime.of(9, 0), 7, AHORA).conHold("h-77", "rp-9", VENCE, AHORA);
    private final ProcesoReserva esperando = conHold.esperandoTelefono(VENCE, AHORA);

    @Test
    void elPedidoGuardaElEventIdYElNuevoVencimiento() {
        ProcesoReserva pedido = esperando.telefonoPedido("ev-1", NUEVO_VENCE, AHORA);

        assertThat(pedido.estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(pedido.requestEventId()).isEqualTo("ev-1");
        assertThat(pedido.venceEn()).isEqualTo(NUEVO_VENCE);
        assertThat(pedido.puedeEnviarTelefono()).isTrue();
    }

    @Test
    void elPedidoResuelveUnProcesoInciertoOAunIniciado() {
        assertThat(conHold.incierto(AHORA).telefonoPedido("ev-1", null, AHORA).estado())
                .isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(conHold.telefonoPedido("ev-1", null, AHORA).venceEn()).isEqualTo(VENCE);
    }

    @Test
    void unProcesoTerminadoOSinHoldNoAceptaElPedido() {
        assertThatThrownBy(() -> esperando.vencido(AHORA).telefonoPedido("ev-1", VENCE, AHORA))
                .isInstanceOf(IllegalStateException.class);
        ProcesoReserva sinHold = ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7,
                AHORA);
        assertThatThrownBy(() -> sinHold.telefonoPedido("ev-1", VENCE, AHORA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinPedidoNoSePuedeMandarElTelefono() {
        assertThat(esperando.puedeEnviarTelefono()).isFalse();
        assertThatThrownBy(() -> esperando.telefonoEnviado(AHORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void despuesDeUnRechazoSePuedeCorregirYMandarOtraVez() {
        ProcesoReserva enviado = esperando.telefonoPedido("ev-1", VENCE, AHORA).telefonoEnviado(AHORA);
        ProcesoReserva rechazado = enviado.telefonoRechazado(NUEVO_VENCE, AHORA);

        assertThat(enviado.estado()).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
        assertThat(enviado.puedeEnviarTelefono()).isFalse();
        assertThat(rechazado.estado()).isEqualTo(EstadoProceso.TELEFONO_RECHAZADO);
        assertThat(rechazado.venceEn()).isEqualTo(NUEVO_VENCE);
        assertThat(rechazado.telefonoEnviado(AHORA).estado()).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
    }

    @Test
    void soloSeRechazaUnTelefonoQueSeMando() {
        assertThatThrownBy(() -> esperando.telefonoRechazado(VENCE, AHORA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void venceCuandoLlegaLaHoraLimite() {
        assertThat(esperando.vencioAntesDe(VENCE.minusSeconds(1))).isFalse();
        assertThat(esperando.vencioAntesDe(VENCE)).isTrue();
        assertThat(ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, AHORA)
                .vencioAntesDe(AHORA)).isFalse();
    }
}
