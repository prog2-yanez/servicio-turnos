package prog2.servicioturnos.reservas.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import prog2.servicioturnos.reservas.domain.model.EstadoProceso;
import prog2.servicioturnos.reservas.domain.model.ProcesoReserva;

// Lo que ve la app de un proceso. Los identificadores de la cátedra (holdId, reservationProcessId) no salen.
public record ProcesoReservaResponse(
        long id,
        EstadoProceso estado,
        long profesionalId,
        LocalDate fecha,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        Instant venceEn) {

    public static ProcesoReservaResponse desde(ProcesoReserva proceso) {
        return new ProcesoReservaResponse(proceso.id(), proceso.estado(), proceso.profesionalId(), proceso.fecha(),
                proceso.horaInicio(), proceso.venceEn());
    }
}
