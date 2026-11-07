package prog2.servicioturnos.reservas.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

// Campos de los eventos del topic de acciones que usa turnos. Los demás (y los que se agreguen) se ignoran.
@JsonIgnoreProperties(ignoreUnknown = true)
record AccionCatedraMensaje(
        String eventId,
        String eventType,
        String reservationProcessId,
        Instant expiresAt,
        String reservationId,
        String reason) {
}
