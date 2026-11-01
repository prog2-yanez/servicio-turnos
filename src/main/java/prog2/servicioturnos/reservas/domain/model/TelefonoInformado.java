package prog2.servicioturnos.reservas.domain.model;

import java.time.Instant;

// El evento AdditionalInformationSubmitted que turnos publica. Cada envío tiene un eventId nuevo.
public record TelefonoInformado(
        String eventId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        Telefono telefono,
        Instant ocurridoEn) {
}
