package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// Una reserva del usuario, armada con lo que sabe turnos y lo que informa la cátedra. procesoId es null si la
// reserva solo la conoce la cátedra; los datos del profesional y horaFin, si solo la conoce turnos.
public record ReservaPropia(
        Long procesoId,
        EstadoProceso estado,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        long profesionalId,
        String profesionalNombre,
        String profesionalApellido,
        Instant confirmadaEn,
        Instant canceladaEn,
        String motivoCancelacion) {
}
