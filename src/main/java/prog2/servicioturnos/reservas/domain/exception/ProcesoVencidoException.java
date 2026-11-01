package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// El hold venció antes de terminar el paso (HOLD_EXPIRED en la cátedra).
public class ProcesoVencidoException extends ConflictoException {

    public ProcesoVencidoException() {
        super("proceso-vencido", "El tiempo para completar la reserva venció; iniciá una nueva");
    }
}
