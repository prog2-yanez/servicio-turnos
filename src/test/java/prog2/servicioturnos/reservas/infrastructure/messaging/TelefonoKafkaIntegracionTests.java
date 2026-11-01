package prog2.servicioturnos.reservas.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import prog2.servicioturnos.TestcontainersConfiguration;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;
import prog2.servicioturnos.reservas.domain.ports.in.EnviarTelefonoUseCase;
import prog2.servicioturnos.reservas.domain.ports.out.ProcesoReservaRepository;
import prog2.servicioturnos.usuarios.domain.model.DatosRegistro;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// De punta a punta con Kafka embebido y la base real: la cátedra se simula publicando sus eventos en el topic de
// acciones y leyendo lo que turnos publica en el topic de teléfono.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "catedra.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "catedra.kafka.consumer-group=turnos-de-prueba",
        "catedra.kafka.topic-acciones=" + TelefonoKafkaIntegracionTests.TOPIC_ACCIONES,
        "catedra.kafka.topic-telefono=" + TelefonoKafkaIntegracionTests.TOPIC_TELEFONO,
        "catedra.group-id=grupo-de-prueba"
})
@EmbeddedKafka(partitions = 1, topics = {
        TelefonoKafkaIntegracionTests.TOPIC_ACCIONES, TelefonoKafkaIntegracionTests.TOPIC_TELEFONO})
class TelefonoKafkaIntegracionTests {

    static final String TOPIC_ACCIONES = "acciones-de-prueba";
    static final String TOPIC_TELEFONO = "telefono-de-prueba";

    private static final Duration ESPERA = Duration.ofSeconds(20);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ProcesoReservaRepository procesos;

    @Autowired
    private EnviarTelefonoUseCase enviarTelefono;

    private Consumer<String, String> catedra;
    private String login;
    private String reservationProcessId;
    private long procesoId;

    @BeforeEach
    void unProcesoEsperandoElPedidoDeTelefono() {
        login = "kafka-" + UUID.randomUUID().toString().substring(0, 8);
        reservationProcessId = "rp-" + UUID.randomUUID();
        Instant ahora = Instant.now();
        long usuarioId = usuarios.guardar(Usuario.registrar(new DatosRegistro(login, "secreta", "Ana", "Gomez",
                login + "@mail.com", null, "es"), "hash", ahora)).id();
        procesoId = procesos.guardar(ProcesoReserva.iniciar(usuarioId, 15, LocalDate.now().plusDays(1),
                        LocalTime.of(9, 0), 7, ahora)
                .conHold("77", reservationProcessId, ahora.plusSeconds(600), ahora)
                .esperandoTelefono(ahora.plusSeconds(600), ahora)).id();
        catedra = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "catedra-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer());
        catedra.subscribe(List.of(TOPIC_TELEFONO));
    }

    @AfterEach
    void cerrar() {
        catedra.close();
    }

    @Test
    void pedidoEnvioRechazoYReenvioDelTelefono() {
        // 1. La cátedra pide el teléfono.
        publicarAccion("ev-pedido-" + reservationProcessId, "AdditionalInformationRequested", "\"PHONE_NUMBER\"");
        await().atMost(ESPERA).until(() -> proceso().requestEventId() != null);
        assertThat(proceso().estado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);

        // 2. El usuario manda su teléfono y turnos lo publica con la key del proceso.
        enviarTelefono.enviar(login, procesoId, "+54 261 555-1234");
        ConsumerRecord<String, String> primero = leerDeLaCatedra();
        JsonNode evento = JSON.readTree(primero.value());
        assertThat(primero.key()).isEqualTo(reservationProcessId);
        assertThat(evento.get("eventType").asString()).isEqualTo("AdditionalInformationSubmitted");
        assertThat(evento.get("groupId").asString()).isEqualTo("grupo-de-prueba");
        assertThat(evento.get("requestEventId").asString()).isEqualTo("ev-pedido-" + reservationProcessId);
        assertThat(evento.get("holdId").asLong()).isEqualTo(77);
        assertThat(evento.get("information").get("phoneNumber").asString()).isEqualTo("+542615551234");
        assertThat(proceso().estado()).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);

        // 3. La cátedra lo rechaza y el usuario lo corrige: sale otro evento con un eventId nuevo.
        publicarAccion("ev-rechazo-" + reservationProcessId, "AdditionalInformationRejected",
                "\"INVALID_PHONE_NUMBER\"");
        await().atMost(ESPERA).until(() -> proceso().estado() == EstadoProceso.TELEFONO_RECHAZADO);
        enviarTelefono.enviar(login, procesoId, "2615559999");
        JsonNode reenvio = JSON.readTree(leerDeLaCatedra().value());
        assertThat(reenvio.get("eventId").asString()).isNotEqualTo(evento.get("eventId").asString());
        assertThat(reenvio.get("information").get("phoneNumber").asString()).isEqualTo("2615559999");
    }

    private void publicarAccion(String eventId, String tipo, String motivo) {
        kafka.send(TOPIC_ACCIONES, reservationProcessId, """
                {"eventId": "%s", "eventType": "%s", "reservationProcessId": "%s", "holdId": 77,
                 "requiredField": %s, "reason": %s, "expiresAt": "%s", "occurredAt": "%s"}
                """.formatted(eventId, tipo, reservationProcessId, motivo, motivo,
                Instant.now().plusSeconds(900), Instant.now())).join();
    }

    private ConsumerRecord<String, String> leerDeLaCatedra() {
        List<ConsumerRecord<String, String>> leidos = new ArrayList<>();
        await().atMost(ESPERA).until(() -> {
            catedra.poll(Duration.ofMillis(200)).forEach(leidos::add);
            return !leidos.isEmpty();
        });
        assertThat(leidos).hasSize(1);
        return leidos.getFirst();
    }

    private ProcesoReserva proceso() {
        return procesos.buscarPorId(procesoId).orElseThrow();
    }
}
