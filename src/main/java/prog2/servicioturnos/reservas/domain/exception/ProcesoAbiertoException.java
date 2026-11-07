package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// Un usuario puede tener un solo proceso de reserva abierto a la vez, así nadie bloquea varios turnos.
public class ProcesoAbiertoException extends ConflictoException {

    public ProcesoAbiertoException() {
        super("proceso-abierto",
                "Ya tenés una reserva en curso; terminala o esperá a que venza antes de iniciar otra");
    }
}
