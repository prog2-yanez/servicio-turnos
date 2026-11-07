package prog2.servicioturnos.reservas.domain.exception;

import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

public class SinProcesoEnCursoException extends RecursoNoEncontradoException {

    public SinProcesoEnCursoException() {
        super("sin-proceso-en-curso", "No tenés ninguna reserva en curso");
    }
}
