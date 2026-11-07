package prog2.servicioturnos.disponibilidad.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// Un turno de un profesional que en la cátedra ya está tomado o retenido. venceEn solo tiene valor si está retenido.
public record Ocupacion(LocalDate fecha, LocalTime horaInicio, EstadoOcupacion estado, Instant venceEn) {
}
