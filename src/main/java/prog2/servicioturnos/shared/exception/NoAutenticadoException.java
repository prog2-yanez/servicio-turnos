package prog2.servicioturnos.shared.exception;

// Quien hace el pedido no demostró quién es: credenciales incorrectas o token inválido. Se responde 401.
public abstract class NoAutenticadoException extends ErrorDeNegocioException {

    protected NoAutenticadoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
