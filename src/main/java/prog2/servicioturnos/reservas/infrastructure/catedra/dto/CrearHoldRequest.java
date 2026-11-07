package prog2.servicioturnos.reservas.infrastructure.catedra.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public record CrearHoldRequest(
        long professionalId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        String externalPatientId) {
}
