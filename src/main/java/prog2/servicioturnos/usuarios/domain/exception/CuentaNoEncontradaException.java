package prog2.servicioturnos.usuarios.domain.exception;

import prog2.servicioturnos.shared.exception.RecursoNoEncontradoException;

// El token es válido pero la cuenta ya no existe.
public class CuentaNoEncontradaException extends RecursoNoEncontradoException {

    public CuentaNoEncontradaException(String login) {
        super("cuenta-no-encontrada", "No existe una cuenta con el login " + login);
    }
}
