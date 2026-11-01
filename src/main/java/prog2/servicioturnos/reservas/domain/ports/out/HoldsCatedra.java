package prog2.servicioturnos.reservas.domain.ports.out;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import prog2.servicioturnos.reservas.domain.model.HoldCreado;

// Retención y confirmación inicial de un turno en la cátedra (contrato, secciones 9 y 10).
// Si la cátedra no contesta, lanza ResultadoInciertoException: no se sabe si el cambio se hizo.
public interface HoldsCatedra {

    HoldCreado crearHold(long profesionalId, LocalDate fecha, LocalTime horaInicio, String externalPatientId);

    // Devuelve el nuevo vencimiento que informa la cátedra.
    Instant confirmar(String holdId, String externalPatientId, String nombre, String apellido);
}
