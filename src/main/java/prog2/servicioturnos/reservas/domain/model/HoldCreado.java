package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;

// Lo que devuelve la cátedra al retener un turno. expiresAt es la hora límite para completar la reserva.
public record HoldCreado(String holdId, String reservationProcessId, Instant venceEn) {
}
