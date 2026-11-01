package prog2.servicioturnos.reservas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import prog2.servicioturnos.TestcontainersConfiguration;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.reservas.infrastructure.persistence.repository.JpaEventoProcesadoRepository;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;

// Kafka embebido y base real: la cátedra manda el mismo resultado dos veces y después uno fuera de orden.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "catedra.kafka.consumer-group=turnos-resultados-de-prueba",
        "catedra.kafka.topic-acciones=" + ResultadosKafkaIntegracionTests.TOPIC_ACCIONES
})
@EmbeddedKafka(partitions = 1, topics = ResultadosKafkaIntegracionTests.TOPIC_ACCIONES)
class ResultadosKafkaIntegracionTests {

    static final String TOPIC_ACCIONES = "acciones-resultados-de-prueba";

    private static final Duration ESPERA = Duration.ofSeconds(20);

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ProcesoReservaRepository procesos;

    @Autowired
    private JpaEventoProcesadoRepository eventosProcesados;

    private String reservationProcessId;
    private long procesoId;

    @BeforeEach
    void unTelefonoEnviado() {
        String login = "resultado-" + UUID.randomUUID().toString().substring(0, 8);
        reservationProcessId = "rp-" + UUID.randomUUID();
        Instant ahora = Instant.now();
        long usuarioId = usuarios.guardar(Usuario.registrar(new DatosRegistro(login, "secreta", "Ana", "Gomez",
                login + "@mail.com", null, "es"), "hash", ahora)).id();
        procesoId = procesos.guardar(ProcesoReserva.iniciar(usuarioId, 15, LocalDate.now().plusDays(1),
                        LocalTime.of(9, 0), 7, ahora)
                .conHold("77", reservationProcessId, ahora.plusSeconds(600), ahora)
                .esperandoTelefono(ahora.plusSeconds(600), ahora)
                .telefonoPedido("ev-pedido-" + reservationProcessId, ahora.plusSeconds(600), ahora)
                .telefonoEnviado(ahora)).id();
    }

    @Test
    void elMismoResultadoDosVecesYUnoFueraDeOrdenNoCambianLaConfirmacion() {
        String confirmado = "ev-confirmado-" + reservationProcessId;
        String vencido = "ev-vencido-" + reservationProcessId;

        publicar(confirmado, "AppointmentConfirmed", "\"reservationId\": 500");
        publicar(confirmado, "AppointmentConfirmed", "\"reservationId\": 500");
        // Con una sola partición los mensajes llegan en orden: cuando se registra este, los anteriores ya pasaron.
        publicar(vencido, "AppointmentProcessExpired", "\"reason\": \"HOLD_EXPIRED\"");

        await().atMost(ESPERA).until(() -> eventosProcesados.existsById(vencido));
        ProcesoReserva proceso = procesos.buscarPorId(procesoId).orElseThrow();
        assertThat(proceso.estado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(proceso.reservationId()).isEqualTo("500");
        assertThat(eventosProcesados.findAll())
                .filteredOn(evento -> evento.getEventId().endsWith(reservationProcessId))
                .extracting(evento -> evento.getTipo())
                .containsExactlyInAnyOrder("AppointmentConfirmed", "AppointmentProcessExpired");
    }

    private void publicar(String eventId, String tipo, String extra) {
        kafka.send(TOPIC_ACCIONES, reservationProcessId, """
                {"eventId": "%s", "eventType": "%s", "reservationProcessId": "%s", %s,
                 "occurredAt": "%s"}
                """.formatted(eventId, tipo, reservationProcessId, extra, Instant.now())).join();
    }
}
