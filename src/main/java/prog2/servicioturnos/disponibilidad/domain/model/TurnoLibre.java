package prog2.servicioturnos.disponibilidad.domain.model;

import java.time.LocalTime;

public record TurnoLibre(LocalTime horaInicio, LocalTime horaFin) {
}
