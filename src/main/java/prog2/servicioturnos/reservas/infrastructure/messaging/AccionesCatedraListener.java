package prog2.servicioturnos.reservas.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.reservas.domain.model.EventoCatedra;
import prog2.servicioturnos.reservas.domain.ports.in.ProcesarAccionesCatedraUseCase;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Escucha el topic de acciones de la cátedra. Solo existe si Kafka está configurado; si no, el servicio arranca
// igual y el resto de la API funciona.
@Component
@ConditionalOnExpression("!'${catedra.kafka.bootstrap-servers:}'.isBlank()"
        + " and !'${catedra.kafka.consumer-group:}'.isBlank()"
        + " and !'${catedra.kafka.topic-acciones:}'.isBlank()")
public class AccionesCatedraListener {

    static final String TELEFONO_PEDIDO = "AdditionalInformationRequested";
    static final String TELEFONO_RECHAZADO = "AdditionalInformationRejected";
    static final String CONFIRMADO = "AppointmentConfirmed";
    static final String VENCIDO = "AppointmentProcessExpired";
    static final String INVALIDO = "AppointmentProcessInvalid";
    static final String CANCELADO = "AppointmentCancelled";

    private static final Logger log = LoggerFactory.getLogger(AccionesCatedraListener.class);

    private final ProcesarAccionesCatedraUseCase procesarAcciones;
    private final JsonMapper json;

    public AccionesCatedraListener(ProcesarAccionesCatedraUseCase procesarAcciones, JsonMapper json) {
        this.procesarAcciones = procesarAcciones;
        this.json = json;
    }

    // El offset se confirma recién cuando este método termina bien; si tira una excepción, Kafka reintenta.
    @KafkaListener(id = "acciones-catedra", topics = "${catedra.kafka.topic-acciones}")
    public void recibir(String mensaje) {
        AccionCatedraMensaje accion;
        try {
            accion = json.readValue(mensaje, AccionCatedraMensaje.class);
        } catch (JacksonException e) {
            // Un mensaje ilegible no se arregla reintentando.
            log.warn("Se descarta un mensaje del topic de acciones que no es un JSON válido");
            return;
        }
        if (accion == null || accion.eventId() == null || accion.reservationProcessId() == null) {
            log.warn("Se descarta un evento de acciones sin eventId o sin reservationProcessId");
            return;
        }
        EventoCatedra evento = new EventoCatedra(accion.eventId(), accion.eventType(),
                accion.reservationProcessId(), accion.expiresAt(), accion.reservationId(), accion.reason());
        switch (String.valueOf(accion.eventType())) {
            case TELEFONO_PEDIDO -> procesarAcciones.telefonoPedido(evento);
            case TELEFONO_RECHAZADO -> procesarAcciones.telefonoRechazado(evento);
            case CONFIRMADO -> procesarAcciones.confirmado(evento);
            case VENCIDO -> procesarAcciones.vencido(evento);
            case INVALIDO -> procesarAcciones.invalido(evento);
            case CANCELADO -> procesarAcciones.cancelado(evento);
            default -> log.info("Se ignora el evento {} de tipo desconocido {}", accion.eventId(), accion.eventType());
        }
    }
}
