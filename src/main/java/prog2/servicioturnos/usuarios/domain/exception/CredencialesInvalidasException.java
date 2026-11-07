package prog2.servicioturnos.usuarios.domain.exception;

import prog2.servicioturnos.shared.exception.NoAutenticadoException;

// El mismo mensaje para usuario inexistente, contraseña errónea o cuenta inactiva: no revela qué cuentas existen.
public class CredencialesInvalidasException extends NoAutenticadoException {

    public CredencialesInvalidasException() {
        super("credenciales-invalidas", "Usuario o contraseña incorrectos");
    }
}
