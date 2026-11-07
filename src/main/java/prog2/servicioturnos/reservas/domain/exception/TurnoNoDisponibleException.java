package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// El turno no está en la agenda vigente, ya pasó, o el profesional dejó de atender.
public class TurnoNoDisponibleException extends ConflictoException {

    public TurnoNoDisponibleException() {
        super("turno-no-disponible", "Ese turno no está disponible; consultá de nuevo los turnos libres");
    }
}
