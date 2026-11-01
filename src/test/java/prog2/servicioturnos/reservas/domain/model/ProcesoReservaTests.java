package prog2.servicioturnos.reservas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ProcesoReservaTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant DESPUES = AHORA.plusSeconds(5);
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final ProcesoReserva iniciado =
            ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, AHORA);

    @Test
    void unProcesoNuevoEmpiezaIniciadoYSinHold() {
        assertThat(iniciado.estado()).isEqualTo(EstadoProceso.INICIADO);
        assertThat(iniciado.id()).isNull();
        assertThat(iniciado.holdId()).isNull();
        assertThat(iniciado.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    void conHoldYConfirmacionQuedaEsperandoElTelefonoConElVencimientoDeLaCatedra() {
        ProcesoReserva conHold = iniciado.conHold("h-1", "rp-1", VENCE, DESPUES);
        ProcesoReserva esperando = conHold.esperandoTelefono(VENCE.plusSeconds(60), DESPUES);

        assertThat(conHold.estado()).isEqualTo(EstadoProceso.INICIADO);
        assertThat(conHold.holdId()).isEqualTo("h-1");
        assertThat(esperando.estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(esperando.reservationProcessId()).isEqualTo("rp-1");
        assertThat(esperando.venceEn()).isEqualTo(VENCE.plusSeconds(60));
        assertThat(esperando.actualizadoEn()).isEqualTo(DESPUES);
        assertThat(esperando.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    void noSeConfirmaUnProcesoSinHold() {
        assertThatThrownBy(() -> iniciado.esperandoTelefono(VENCE, DESPUES))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void soloUnProcesoIniciadoRecibeElHold() {
        ProcesoReserva incierto = iniciado.incierto(DESPUES);

        assertThat(incierto.estado()).isEqualTo(EstadoProceso.INCIERTO);
        assertThatThrownBy(() -> incierto.conHold("h-1", "rp-1", VENCE, DESPUES))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unProcesoTerminadoNoSeReabre() {
        ProcesoReserva rechazado = iniciado.rechazado(DESPUES);
        ProcesoReserva vencido = iniciado.vencido(DESPUES);

        assertThat(rechazado.estado().esFinal()).isTrue();
        assertThat(vencido.estado()).isEqualTo(EstadoProceso.VENCIDO);
        assertThatThrownBy(() -> rechazado.incierto(DESPUES)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> vencido.rechazado(DESPUES)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elPacienteNecesitaNombreYApellidoDeAlMenosDosLetras() {
        assertThat(new Paciente(4, "Ana", "Gomez").tieneNombreCompleto()).isTrue();
        assertThat(new Paciente(4, "Ana", "Gomez").externalPatientId()).isEqualTo("4");
        assertThat(new Paciente(4, null, "Gomez").tieneNombreCompleto()).isFalse();
        assertThat(new Paciente(4, "Ana", " G ").tieneNombreCompleto()).isFalse();
        assertThat(new Paciente(4, "A".repeat(101), "Gomez").tieneNombreCompleto()).isFalse();
    }
}
