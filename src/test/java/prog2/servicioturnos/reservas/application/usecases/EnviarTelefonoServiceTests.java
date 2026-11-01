package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.exception.EnvioNoConfirmadoException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoNoEncontradoException;
import prog2.servicioturnos.reservas.domain.exception.ProcesoVencidoException;
import prog2.servicioturnos.reservas.domain.exception.TelefonoInvalidoException;
import prog2.servicioturnos.reservas.domain.exception.TelefonoNoPedidoException;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.TelefonoInformado;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.PublicadorTelefono;

class EnviarTelefonoServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final CuentasPacientes cuentas = mock(CuentasPacientes.class);
    private final PublicadorTelefono publicador = mock(PublicadorTelefono.class);
    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final EnviarTelefonoService servicio =
            new EnviarTelefonoService(cuentas, procesos, publicador, Clock.fixed(AHORA, ZoneOffset.UTC));

    private long procesoId;

    @BeforeEach
    void unProcesoEsperandoElTelefono() {
        when(cuentas.obtener("ana")).thenReturn(new Paciente(4, "Ana", "Gomez"));
        procesoId = procesos.guardar(proceso(4).telefonoPedido("ev-pedido", VENCE, AHORA)).id();
    }

    @Test
    void publicaElTelefonoNormalizadoConElPedidoYQuedaEnviado() {
        ProcesoReserva enviado = servicio.enviar("ana", procesoId, "+54 261 555-1234");

        ArgumentCaptor<TelefonoInformado> evento = ArgumentCaptor.forClass(TelefonoInformado.class);
        verify(publicador).publicar(evento.capture());
        assertThat(evento.getValue().telefono().numero()).isEqualTo("+542615551234");
        assertThat(evento.getValue().reservationProcessId()).isEqualTo("rp-9");
        assertThat(evento.getValue().holdId()).isEqualTo("77");
        assertThat(evento.getValue().requestEventId()).isEqualTo("ev-pedido");
        assertThat(evento.getValue().eventId()).isNotBlank();
        assertThat(evento.getValue().ocurridoEn()).isEqualTo(AHORA);
        assertThat(enviado.estado()).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
    }

    @Test
    void cadaEnvioTieneUnEventIdNuevo() {
        servicio.enviar("ana", procesoId, "2615551234");
        procesos.guardar(procesos.ultimo().telefonoRechazado(VENCE, AHORA));
        servicio.enviar("ana", procesoId, "2615559999");

        ArgumentCaptor<TelefonoInformado> eventos = ArgumentCaptor.forClass(TelefonoInformado.class);
        verify(publicador, times(2)).publicar(eventos.capture());
        assertThat(eventos.getAllValues().get(0).eventId()).isNotEqualTo(eventos.getAllValues().get(1).eventId());
        assertThat(eventos.getAllValues()).allSatisfy(e -> assertThat(e.requestEventId()).isEqualTo("ev-pedido"));
    }

    @Test
    void elProcesoDeOtroUsuarioSeVeComoInexistente() {
        when(cuentas.obtener("luis")).thenReturn(new Paciente(5, "Luis", "Paz"));

        assertThatThrownBy(() -> servicio.enviar("luis", procesoId, "2615551234"))
                .isInstanceOf(ProcesoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.enviar("ana", 999, "2615551234"))
                .isInstanceOf(ProcesoNoEncontradoException.class);
        verifyNoInteractions(publicador);
    }

    @Test
    void sinPedidoDeTelefonoResponde409() {
        long sinPedido = procesos.guardar(proceso(4).vencido(AHORA)).id();

        assertThatThrownBy(() -> servicio.enviar("ana", sinPedido, "2615551234"))
                .isInstanceOf(TelefonoNoPedidoException.class);
        verifyNoInteractions(publicador);
    }

    @Test
    void siYaVencioLoMarcaVencidoSinPublicar() {
        EnviarTelefonoService tarde = new EnviarTelefonoService(cuentas, procesos, publicador,
                Clock.fixed(VENCE.plusSeconds(1), ZoneOffset.UTC));

        assertThatThrownBy(() -> tarde.enviar("ana", procesoId, "2615551234"))
                .isInstanceOf(ProcesoVencidoException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.VENCIDO);
        verifyNoInteractions(publicador);
    }

    @Test
    void unTelefonoInvalidoNoSePublica() {
        assertThatThrownBy(() -> servicio.enviar("ana", procesoId, "123"))
                .isInstanceOf(TelefonoInvalidoException.class);
        verifyNoInteractions(publicador);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
    }

    @Test
    void siKafkaNoConfirmaElProcesoQuedaComoEstabaParaReintentar() {
        doThrow(new EnvioNoConfirmadoException()).when(publicador).publicar(any());

        assertThatThrownBy(() -> servicio.enviar("ana", procesoId, "2615551234"))
                .isInstanceOf(EnvioNoConfirmadoException.class);
        assertThat(procesos.ultimo().estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
    }

    private static ProcesoReserva proceso(long usuarioId) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, AHORA)
                .conHold("77", "rp-9", VENCE, AHORA)
                .esperandoTelefono(VENCE, AHORA);
    }
}
