package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// Otro usuario lo retuvo o lo reservó antes (SLOT_ALREADY_HELD o SLOT_ALREADY_RESERVED en la cátedra).
public class TurnoOcupadoException extends ConflictoException {

    public TurnoOcupadoException() {
        super("turno-ocupado", "Ese turno ya no está libre; elegí otro");
    }
}
