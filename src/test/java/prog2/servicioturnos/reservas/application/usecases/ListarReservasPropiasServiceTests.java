package prog2.servicioturnos.reservas.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prog2.servicioturnos.disponibilidad.domain.exception.CatedraNoDisponibleException;
import prog2.servicioturnos.reservas.RepositorioProcesosEnMemoria;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.EstadoReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.Paciente;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.model.ReservaCatedra;
import prog2.servicioturnos.reservas.domain.model.ReservaPropia;
import prog2.servicioturnos.reservas.domain.model.ReservasPropias;
import prog2.servicioturnos.reservas.domain.ports.out.CuentasPacientes;
import prog2.servicioturnos.reservas.domain.ports.out.ReservasCatedra;

class ListarReservasPropiasServiceTests {

    private static final Instant AHORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-02T12:10:00Z");

    private final CuentasPacientes cuentas = mock(CuentasPacientes.class);
    private final ReservasCatedra catedra = mock(ReservasCatedra.class);
    private final RepositorioProcesosEnMemoria procesos = new RepositorioProcesosEnMemoria();
    private final ListarReservasPropiasService servicio = new ListarReservasPropiasService(cuentas, procesos, catedra);

    @BeforeEach
    void dosUsuarios() {
        when(cuentas.obtener("ana")).thenReturn(new Paciente(4, "Ana", "Gomez"));
        when(cuentas.obtener("luis")).thenReturn(new Paciente(5, "Luis", "Paz"));
    }

    @Test
    void cadaUsuarioVeSoloSusReservasYSeLePideALaCatedraSoloLasSuyas() {
        procesos.guardar(confirmado(4, "rp-ana", "09:00"));
        procesos.guardar(confirmado(5, "rp-luis", "10:00"));
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                deLaCatedra("rp-ana", "4", "09:00", EstadoReservaCatedra.CONFIRMADA)));
        when(catedra.deUnPaciente("5")).thenReturn(List.of(
                deLaCatedra("rp-luis", "5", "10:00", EstadoReservaCatedra.CONFIRMADA)));

        ReservasPropias deAna = servicio.listar("ana");

        assertThat(deAna.reservas()).extracting(ReservaPropia::horaInicio).containsExactly(LocalTime.of(9, 0));
        assertThat(deAna.desactualizado()).isFalse();
        verify(catedra).deUnPaciente("4");
        assertThat(servicio.listar("luis").reservas()).extracting(ReservaPropia::horaInicio)
                .containsExactly(LocalTime.of(10, 0));
    }

    @Test
    void combinaLoLocalConLaCatedraYMandaLaCatedra() {
        ProcesoReserva local = procesos.guardar(confirmado(4, "rp-ana", "09:00"));
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                deLaCatedra("rp-ana", "4", "09:00", EstadoReservaCatedra.CANCELADA)));

        ReservaPropia reserva = servicio.listar("ana").reservas().getFirst();

        assertThat(reserva.procesoId()).isEqualTo(local.id());
        assertThat(reserva.estado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(reserva.horaFin()).isEqualTo(LocalTime.of(9, 30));
        assertThat(reserva.profesionalNombre()).isEqualTo("Ana");
        // Mostrar lo de la cátedra no cambia la base: eso lo hace la recuperación.
        assertThat(procesos.buscarPorId(local.id()).orElseThrow().estado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void mientrasEsperaElTelefonoSeMuestraElPasoLocal() {
        procesos.guardar(esperando(4, "rp-ana").telefonoPedido("ev-1", VENCE, AHORA).telefonoEnviado(AHORA));
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                deLaCatedra("rp-ana", "4", "09:00", EstadoReservaCatedra.TELEFONO_PENDIENTE),
                deLaCatedra("rp-sin-local", "4", "11:00", EstadoReservaCatedra.TELEFONO_PENDIENTE)));

        List<ReservaPropia> reservas = servicio.listar("ana").reservas();

        assertThat(reservas).extracting(ReservaPropia::horaInicio, ReservaPropia::estado, ReservaPropia::procesoId)
                .containsExactly(
                        tuple(LocalTime.of(11, 0), EstadoProceso.ESPERANDO_TELEFONO,
                                null),
                        tuple(LocalTime.of(9, 0), EstadoProceso.TELEFONO_ENVIADO,
                                procesos.ultimo().id()));
    }

    @Test
    void soloListaReservasDeVerdad() {
        procesos.guardar(ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 3), LocalTime.of(8, 0), 7, AHORA)
                .rechazado(AHORA));
        procesos.guardar(ProcesoReserva.iniciar(4, 15, LocalDate.of(2026, 11, 4), LocalTime.of(8, 0), 7, AHORA));
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                deLaCatedra("rp-fallida", "4", "12:00", EstadoReservaCatedra.FALLIDA)));

        assertThat(servicio.listar("ana").reservas()).isEmpty();
    }

    @Test
    void siLaCatedraNoRespondeDevuelveLoLocalAvisandoQuePuedeEstarDesactualizado() {
        procesos.guardar(confirmado(4, "rp-ana", "09:00"));
        procesos.guardar(confirmado(4, "rp-ana-2", "10:00").cancelado(null, "Pedido del paciente", AHORA));
        procesos.guardar(confirmado(5, "rp-luis", "11:00"));
        when(catedra.deUnPaciente("4")).thenThrow(new CatedraNoDisponibleException());

        ReservasPropias deAna = servicio.listar("ana");

        assertThat(deAna.desactualizado()).isTrue();
        assertThat(deAna.reservas()).extracting(ReservaPropia::estado, ReservaPropia::motivoCancelacion)
                .containsExactly(
                        tuple(EstadoProceso.CANCELADO, "Pedido del paciente"),
                        tuple(EstadoProceso.CONFIRMADO, null));
    }

    @Test
    void unaReservaQueLaCatedraNoDevolvioSeMuestraConLoLocal() {
        procesos.guardar(confirmado(4, "rp-ana", "09:00"));
        when(catedra.deUnPaciente("4")).thenReturn(List.of());

        ReservaPropia reserva = servicio.listar("ana").reservas().getFirst();

        assertThat(reserva.estado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(reserva.horaFin()).isNull();
        assertThat(reserva.profesionalNombre()).isNull();
    }

    @Test
    void ordenaDeLaMasNuevaALaMasVieja() {
        when(catedra.deUnPaciente("4")).thenReturn(List.of(
                deLaCatedra("rp-1", "4", "09:00", EstadoReservaCatedra.CONFIRMADA),
                new ReservaCatedra("501", "rp-2", "4", LocalDate.of(2026, 11, 10), LocalTime.of(8, 0),
                        LocalTime.of(8, 30), 15, "Ana", "Gomez", EstadoReservaCatedra.CONFIRMADA, AHORA, null, null),
                deLaCatedra("rp-3", "4", "17:00", EstadoReservaCatedra.CONFIRMADA)));

        assertThat(servicio.listar("ana").reservas()).extracting(ReservaPropia::fecha, ReservaPropia::horaInicio)
                .containsExactly(
                        tuple(LocalDate.of(2026, 11, 10), LocalTime.of(8, 0)),
                        tuple(LocalDate.of(2026, 11, 3), LocalTime.of(17, 0)),
                        tuple(LocalDate.of(2026, 11, 3), LocalTime.of(9, 0)));
    }

    private static ProcesoReserva esperando(long usuarioId, String reservationProcessId) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.of(9, 0), 7, AHORA)
                .conHold("77", reservationProcessId, VENCE, AHORA)
                .esperandoTelefono(VENCE, AHORA);
    }

    private static ProcesoReserva confirmado(long usuarioId, String reservationProcessId, String hora) {
        return ProcesoReserva.iniciar(usuarioId, 15, LocalDate.of(2026, 11, 3), LocalTime.parse(hora), 7, AHORA)
                .conHold("77", reservationProcessId, VENCE, AHORA)
                .esperandoTelefono(VENCE, AHORA)
                .confirmado("res-" + reservationProcessId, AHORA);
    }

    private static ReservaCatedra deLaCatedra(String reservationProcessId, String paciente, String hora,
            EstadoReservaCatedra estado) {
        LocalTime inicio = LocalTime.parse(hora);
        return new ReservaCatedra("res-" + reservationProcessId, reservationProcessId, paciente,
                LocalDate.of(2026, 11, 3), inicio, inicio.plusMinutes(30), 15, "Ana", "Gomez", estado, AHORA, null,
                null);
    }
}
