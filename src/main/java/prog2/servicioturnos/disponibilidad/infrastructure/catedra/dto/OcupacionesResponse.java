package prog2.servicioturnos.disponibilidad.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Respuesta de GET /api/appointment-occupancies de la cátedra (contrato, sección 8).
@JsonIgnoreProperties(ignoreUnknown = true)
public record OcupacionesResponse(List<Ocupacion> occupancies) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ocupacion(LocalDate date, LocalTime startTime, String status, Instant expiresAt) {
    }
}
