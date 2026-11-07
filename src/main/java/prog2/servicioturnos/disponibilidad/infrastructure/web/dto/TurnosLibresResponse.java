package prog2.servicioturnos.disponibilidad.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import prog2.servicioturnos.disponibilidad.domain.model.TurnosLibresDelProfesional;

public record TurnosLibresResponse(long profesionalId, LocalDate fecha, long versionCatalogo, List<Turno> turnos) {

    public static TurnosLibresResponse desde(TurnosLibresDelProfesional libres) {
        return new TurnosLibresResponse(libres.profesionalId(), libres.fecha(), libres.versionCatalogo(),
                libres.turnos().stream().map(turno -> new Turno(turno.horaInicio(), turno.horaFin())).toList());
    }

    public record Turno(@JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                        @JsonFormat(pattern = "HH:mm") LocalTime horaFin) {
    }
}
