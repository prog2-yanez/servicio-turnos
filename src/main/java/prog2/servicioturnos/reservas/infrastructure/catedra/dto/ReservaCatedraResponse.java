package prog2.servicioturnos.reservas.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// Un elemento de GET /api/appointments. Los datos personales del paciente (nombre, apellido y teléfono) no se leen.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservaCatedraResponse(
        String reservationId,
        String reservationProcessId,
        String externalPatientId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        Long professionalId,
        String professionalFirstName,
        String professionalLastName,
        String status,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason) {
}
