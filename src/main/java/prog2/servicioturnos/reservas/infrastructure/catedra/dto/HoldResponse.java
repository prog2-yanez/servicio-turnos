package prog2.servicioturnos.reservas.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

// Respuesta del hold (201) y de la confirmación inicial (202). La confirmación no trae holdId.
@JsonIgnoreProperties(ignoreUnknown = true)
public record HoldResponse(String holdId, String reservationProcessId, String status, Instant expiresAt) {
}
