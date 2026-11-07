package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// La cátedra rechazó el pedido por otro motivo definitivo (por ejemplo, datos que no coinciden).
public class ReservaRechazadaException extends ConflictoException {

    public ReservaRechazadaException() {
        super("reserva-rechazada", "El servicio central rechazó la reserva; probá con otro turno");
    }
}
