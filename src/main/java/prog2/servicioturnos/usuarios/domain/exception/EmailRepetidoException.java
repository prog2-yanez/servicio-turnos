package prog2.servicioturnos.usuarios.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

public class EmailRepetidoException extends ConflictoException {

    public EmailRepetidoException() {
        super("email-repetido", "Ya existe una cuenta con ese email");
    }
}
