package prog2.servicioturnos.reservas.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// Body de POST /api/appointments/{reservationProcessId}/cancel. Sin motivo se manda vacío.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CancelarReservaRequest(String reason) {
}
