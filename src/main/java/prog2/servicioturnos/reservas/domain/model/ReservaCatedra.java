package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

// Una reserva tal como la informa la cátedra. Los datos del paciente (nombre y teléfono) no se traen: turnos ya los
// conoce y no hace falta mostrarlos.
public record ReservaCatedra(
        String reservationId,
        String reservationProcessId,
        String externalPatientId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        long profesionalId,
        String profesionalNombre,
        String profesionalApellido,
        EstadoReservaCatedra estado,
        Instant confirmadaEn,
        Instant canceladaEn,
        String motivoCancelacion) {
}
