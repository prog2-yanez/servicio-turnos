package prog2.servicioturnos.disponibilidad.domain.model;

import java.time.LocalDate;
import java.util.List;

// Turnos que se le pueden ofrecer al usuario. versionCatalogo dice con qué versión del catálogo se armaron.
public record TurnosLibresDelProfesional(
        long profesionalId, LocalDate fecha, long versionCatalogo, List<TurnoLibre> turnos) {

    public TurnosLibresDelProfesional {
        turnos = List.copyOf(turnos);
    }
}
