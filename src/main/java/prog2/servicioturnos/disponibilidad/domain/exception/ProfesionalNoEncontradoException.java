package prog2.servicioturnos.disponibilidad.domain.exception;

import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

public class ProfesionalNoEncontradoException extends RecursoNoEncontradoException {

    public ProfesionalNoEncontradoException(long id) {
        super("profesional-no-encontrado", "No existe un profesional con id " + id);
    }
}
