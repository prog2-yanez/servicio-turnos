package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

// Se usa también cuando el proceso es de otro usuario: así no se revela que existe.
public class ProcesoNoEncontradoException extends RecursoNoEncontradoException {

    public ProcesoNoEncontradoException(long id) {
        super("proceso-no-encontrado", "No tenés una reserva con id " + id);
    }
}
