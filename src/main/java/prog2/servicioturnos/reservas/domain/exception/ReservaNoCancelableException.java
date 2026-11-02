package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// Solo se cancela una reserva confirmada (RESERVATION_NOT_CANCELLABLE en la cátedra). Un turno retenido todavía sin
// confirmar se deja vencer.
public class ReservaNoCancelableException extends ConflictoException {

    public ReservaNoCancelableException() {
        super("reserva-no-cancelable", "Solo se puede cancelar una reserva confirmada");
    }
}
