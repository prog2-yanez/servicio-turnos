package prog2.servicioturnos.reservas.infrastructure.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.reservas.domain.exception.EnvioNoConfirmadoException;
import prog2.servicioturnos.reservas.domain.model.TelefonoInformado;
import prog2.servicioturnos.reservas.domain.ports.out.PublicadorTelefono;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// Publica AdditionalInformationSubmitted en el topic de teléfono (contrato, sección 15.5). La message key es el
// reservationProcessId, así todos los eventos de un mismo proceso van a la misma partición y en orden.
@Component
public class PublicadorTelefonoKafka implements PublicadorTelefono {

    static final String TIPO_EVENTO = "AdditionalInformationSubmitted";
    static final long ESPERA_CONFIRMACION_SEGUNDOS = 10;

    private static final Logger log = LoggerFactory.getLogger(PublicadorTelefonoKafka.class);

    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;
    private final String topic;
    private final String groupId;

    public PublicadorTelefonoKafka(KafkaTemplate<String, String> kafka, JsonMapper json,
            @Value("${catedra.kafka.topic-telefono:}") String topic, @Value("${catedra.group-id:}") String groupId) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
        this.groupId = groupId;
    }

    @Override
    public void publicar(TelefonoInformado telefono) {
        if (topic.isBlank() || groupId.isBlank()) {
            log.error("No se puede publicar el teléfono: "
                    + "falta configurar CATEDRA_KAFKA_TOPIC_TELEFONO o CATEDRA_GROUP_ID");
            throw new EnvioNoConfirmadoException();
        }
        try {
            kafka.send(topic, telefono.reservationProcessId(), mensaje(telefono))
                    .get(ESPERA_CONFIRMACION_SEGUNDOS, TimeUnit.SECONDS);
            log.info("Teléfono publicado con el evento {}", telefono.eventId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnvioNoConfirmadoException();
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            // El mensaje de error no se loguea: podría incluir el contenido del evento, con el teléfono.
            log.warn("Kafka no confirmó el evento {}: {}", telefono.eventId(), e.getClass().getSimpleName());
            throw new EnvioNoConfirmadoException();
        }
    }

    private String mensaje(TelefonoInformado telefono) {
        ObjectNode evento = json.createObjectNode();
        evento.put("eventId", telefono.eventId());
        evento.put("eventType", TIPO_EVENTO);
        evento.put("groupId", groupId);
        evento.put("reservationProcessId", telefono.reservationProcessId());
        ponerIdentificador(evento, "holdId", telefono.holdId());
        evento.put("requestEventId", telefono.requestEventId());
        evento.putObject("information").put("phoneNumber", telefono.telefono().numero());
        evento.put("occurredAt", telefono.ocurridoEn().toString());
        evento.put("schemaVersion", 1);
        return json.writeValueAsString(evento);
    }

    // La cátedra devuelve el holdId como número; se guarda como texto y se manda con el mismo tipo que llegó.
    private static void ponerIdentificador(ObjectNode evento, String campo, String valor) {
        if (valor != null && valor.matches("\\d{1,18}")) {
            evento.put(campo, Long.parseLong(valor));
        } else {
            evento.put(campo, valor);
        }
    }
}
