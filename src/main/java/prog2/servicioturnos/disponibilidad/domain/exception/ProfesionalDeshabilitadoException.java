package prog2.servicioturnos.disponibilidad.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

// El profesional existe pero no atiende: no se le pueden dar turnos.
public class ProfesionalDeshabilitadoException extends ConflictoException {

    public ProfesionalDeshabilitadoException(long id) {
        super("profesional-deshabilitado", "El profesional con id " + id + " está deshabilitado");
    }
}
