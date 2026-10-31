package prog2.servicioturnos.disponibilidad.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

// Franja en la que atiende un profesional cada semana, partida en turnos de igual duración.
public record HorarioSemanal(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin, int duracionTurnoMinutos) {
}
