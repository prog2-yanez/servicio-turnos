package prog2.servicioturnos.reservas.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

// Respuesta 200 de la cancelación. Si la reserva ya estaba cancelada, la cátedra devuelve su estado actual.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CancelacionResponse(String reservationId, String reservationProcessId, String status,
        Instant cancelledAt) {
}
