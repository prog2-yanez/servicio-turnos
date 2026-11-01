package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;

// Lo que turnos usa de un evento del topic de acciones de la cátedra. venceEn puede no venir.
public record EventoCatedra(String eventId, String reservationProcessId, Instant venceEn) {
}
