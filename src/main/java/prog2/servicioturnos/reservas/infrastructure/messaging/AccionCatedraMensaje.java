package prog2.servicioturnos.reservas.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

// Campos comunes de los eventos del topic de acciones. Los demás (y los que se agreguen) se ignoran.
@JsonIgnoreProperties(ignoreUnknown = true)
record AccionCatedraMensaje(String eventId, String eventType, String reservationProcessId, Instant expiresAt) {
}
