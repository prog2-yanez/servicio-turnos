package prog2.servicioturnos.reservas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ProcesoReservaResultadoTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final ProcesoReserva enviado = ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3),
                    LocalTime.of(9, 0), 7, AHORA)
            .conHold("77", "rp-9", VENCE, AHORA)
            .esperandoTelefono(VENCE, AHORA)
            .telefonoPedido("ev-pedido", VENCE, AHORA)
            .telefonoEnviado(AHORA);

    @Test
    void laConfirmacionGuardaElIdDeLaReserva() {
        ProcesoReserva confirmado = enviado.confirmado("res-500", AHORA);

        assertThat(confirmado.estado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(confirmado.reservationId()).isEqualTo("res-500");
    }

    @Test
    void laCatedraPuedeConfirmarLoQueTurnosHabiaDadoPorVencido() {
        assertThat(enviado.vencido(AHORA).confirmado("res-500", AHORA).estado())
                .isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void otrosEstadosFinalesNoSeConfirman() {
        assertThatThrownBy(() -> enviado.invalido("SLOT_NOT_AVAILABLE", AHORA).confirmado("res-500", AHORA))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> enviado.cancelado(null, null, AHORA).confirmado("res-500", AHORA))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> enviado.rechazado(AHORA).confirmado("res-500", AHORA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laInvalidacionGuardaElMotivoYSoloAplicaAUnProcesoAbierto() {
        ProcesoReserva invalido = enviado.invalido("SLOT_NOT_AVAILABLE", AHORA);

        assertThat(invalido.estado()).isEqualTo(EstadoProceso.INVALIDO);
        assertThat(invalido.motivo()).isEqualTo("SLOT_NOT_AVAILABLE");
        assertThatThrownBy(() -> enviado.confirmado("res-500", AHORA).invalido("TARDE", AHORA))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unaReservaConfirmadaSePuedeCancelarYConservaSuId() {
        ProcesoReserva cancelado = enviado.confirmado("res-500", AHORA).cancelado("otro", "Pedido del paciente", AHORA);

        assertThat(cancelado.estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(cancelado.reservationId()).isEqualTo("res-500");
        assertThat(cancelado.motivo()).isEqualTo("Pedido del paciente");
    }

    @Test
    void unProcesoAbiertoSePuedeCancelarConElIdQueInformaLaCatedra() {
        ProcesoReserva cancelado = enviado.cancelado("res-500", null, AHORA);

        assertThat(cancelado.estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(cancelado.reservationId()).isEqualTo("res-500");
        assertThat(cancelado.motivo()).isNull();
    }

    @Test
    void unProcesoVencidoNoSeCancela() {
        assertThatThrownBy(() -> enviado.vencido(AHORA).cancelado("res-500", null, AHORA))
                .isInstanceOf(IllegalStateException.class);
    }
}
