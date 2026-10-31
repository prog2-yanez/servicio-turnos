package prog2.servicioturnos.usuarios.domain.exception;

import prog2.servicioturnos.shared.exception.ConflictoException;

public class LoginRepetidoException extends ConflictoException {

    public LoginRepetidoException(String login) {
        super("login-repetido", "Ya existe una cuenta con el login " + login);
    }
}
