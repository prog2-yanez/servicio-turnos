package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;

// Lo que turnos usa de un evento del topic de acciones de la cátedra. venceEn, reservationId y motivo solo vienen
// en algunos tipos de evento.
public record EventoCatedra(
        String eventId,
        String tipo,
        String reservationProcessId,
        Instant venceEn,
        String reservationId,
        String motivo) {
}
